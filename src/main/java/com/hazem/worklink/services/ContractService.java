package com.hazem.worklink.services;

import com.hazem.worklink.dto.response.ContractResponse;
import com.hazem.worklink.exceptions.ResourceNotFoundException;
import com.hazem.worklink.models.Company;
import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.Freelancer;
import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.ContractStatus;
import com.hazem.worklink.repositories.CompanyRepository;
import com.hazem.worklink.repositories.ContractRepository;
import com.hazem.worklink.repositories.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final ActiveMissionService activeMissionService;
    private final MessageService messageService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path contractsPath;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @PostConstruct
    public void init() {
        try {
            contractsPath = Paths.get(uploadDir, "contracts").toAbsolutePath().normalize();
            Files.createDirectories(contractsPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create contracts directory", e);
        }
    }

    // ─── Generate contract when application is accepted ─────────────────────────

    public Contract generateContract(Mission mission, Freelancer freelancer, Company company) {
        // Avoid duplicates
        if (contractRepository.existsByJobIdAndFreelancerId(mission.getId(), freelancer.getId())) {
            log.info("Contract already exists for mission {} and freelancer {}", mission.getId(), freelancer.getId());
            return null;
        }

        Contract contract = new Contract();
        contract.setJobId(mission.getId());
        contract.setFreelancerId(freelancer.getId());
        contract.setCompanyId(company.getId());
        contract.setFreelancerName(freelancer.getFirstName() + " " + freelancer.getLastName());
        contract.setFreelancerEmail(freelancer.getEmail());
        contract.setCompanyName(company.getCompanyName());
        contract.setCompanyEmail(company.getEmail());
        contract.setMissionTitle(mission.getJobTitle());
        contract.setSalary(mission.getTjm());
        contract.setStartDate(mission.getStartDate() != null ? mission.getStartDate() : LocalDate.now().plusDays(7));
        contract.setEndDate(mission.getEndDate());
        contract.setTerms(buildDefaultTerms(mission, freelancer, company));
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setCreatedAt(LocalDateTime.now());

        // Auto-sign on behalf of the company: generate signature image with company name + stamp
        contract.setCompanySignedAt(LocalDateTime.now());
        try {
            contract.setCompanySignatureImageBase64(generateCompanyAutoSignature(company.getCompanyName()));
        } catch (Exception e) {
            log.warn("Could not generate company auto-signature image: {}", e.getMessage());
        }

        // Save first to get ID
        Contract saved = contractRepository.save(contract);

        // Generate PDF with company auto-signature already embedded
        try {
            String pdfUrl = generateContractPdf(saved, true);
            saved.setPdfUrl(pdfUrl);
            saved = contractRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to generate contract PDF: {}", e.getMessage());
        }

        // Notify freelancer
        try {
            notificationService.sendContractGeneratedNotification(
                    freelancer.getId(), mission.getJobTitle(), company.getCompanyName(), saved.getId());
        } catch (Exception e) {
            log.error("Failed to send contract generated notification: {}", e.getMessage());
        }

        // Notify company
        try {
            notificationService.sendContractCreatedToCompanyNotification(
                    company.getId(), mission.getJobTitle(), freelancer.getFirstName() + " " + freelancer.getLastName());
        } catch (Exception e) {
            log.error("Failed to send contract created company notification: {}", e.getMessage());
        }

        log.info("Contract generated: {} for mission {}", saved.getId(), mission.getJobTitle());
        return saved;
    }

    // ─── Sign contract ────────────────────────────────────────────────────────

    public ContractResponse signContract(String contractId, String freelancerEmail, String signatureBase64) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + contractId));

        if (!contract.getFreelancerId().equals(freelancer.getId())) {
            throw new RuntimeException("Unauthorized: contract does not belong to this freelancer");
        }

        if (contract.getSignedAt() != null) {
            throw new RuntimeException("Contract is already signed by the freelancer");
        }

        contract.setSignatureImageBase64(signatureBase64);
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());

        // Generate fully signed PDF (both signatures)
        try {
            String signedPdfUrl = generateContractPdf(contract, true);
            contract.setSignedPdfUrl(signedPdfUrl);
        } catch (Exception e) {
            log.error("Failed to generate signed PDF: {}", e.getMessage());
        }

        Contract saved = contractRepository.save(contract);

        // Trigger active mission creation
        try {
            activeMissionService.createFromContract(saved);
        } catch (Exception e) {
            log.error("Failed to create active mission from contract {}: {}", saved.getId(), e.getMessage());
        }

        // Create messaging conversation — isolated so any prior failure never blocks this
        try {
            messageService.ensureConversationFromContract(saved.getCompanyId(), saved.getFreelancerId());
            log.info("Conversation ensured for company {} and freelancer {}", saved.getCompanyId(), saved.getFreelancerId());
        } catch (Exception e) {
            log.error("Failed to ensure conversation for contract {}: {}", saved.getId(), e.getMessage());
        }

        // Notify company that freelancer signed
        try {
            Company company = companyRepository.findById(contract.getCompanyId()).orElse(null);
            if (company != null) {
                notificationService.sendContractSignedNotification(
                        company.getId(), contract.getMissionTitle(),
                        contract.getFreelancerName(), contractId,
                        saved.getSignedPdfUrl());
            }
        } catch (Exception e) {
            log.error("Failed to send company notification for contract {}: {}", saved.getId(), e.getMessage());
        }

        // Notify freelancer
        try {
            notificationService.sendContractSignedToFreelancerNotification(
                    freelancer.getId(), contract.getMissionTitle(), saved.getSignedPdfUrl());
        } catch (Exception e) {
            log.error("Failed to send freelancer notification for contract {}: {}", saved.getId(), e.getMessage());
        }

        return ContractResponse.from(saved);
    }

    // ─── Company signs contract ───────────────────────────────────────────────

    public ContractResponse signContractAsCompany(String contractId, String companyEmail, String signatureBase64) {
        Company company = companyRepository.findByEmail(companyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + contractId));

        if (!contract.getCompanyId().equals(company.getId())) {
            throw new RuntimeException("Unauthorized: contract does not belong to this company");
        }
        if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new RuntimeException("Contract is not in a signable state");
        }
        if (contract.getCompanySignedAt() != null) {
            throw new RuntimeException("Contract is already signed by the company");
        }

        contract.setCompanySignatureImageBase64(signatureBase64);
        contract.setCompanySignedAt(LocalDateTime.now());

        // Generate PDF with company signature — freelancer will add theirs later
        try {
            String signedPdfUrl = generateContractPdf(contract, true);
            contract.setSignedPdfUrl(signedPdfUrl);
        } catch (Exception e) {
            log.error("Failed to generate company-signed PDF: {}", e.getMessage());
        }

        Contract saved = contractRepository.save(contract);

        // Notify freelancer that company has signed and it's their turn
        Freelancer freelancer = freelancerRepository.findById(contract.getFreelancerId()).orElse(null);
        if (freelancer != null) {
            notificationService.sendContractGeneratedNotification(
                    freelancer.getId(), contract.getMissionTitle(), company.getCompanyName(), saved.getId());
        }

        return ContractResponse.from(saved);
    }

    // ─── Reject contract (by freelancer) ─────────────────────────────────────

    public ContractResponse rejectContract(String contractId, String freelancerEmail, String reason) {
        Freelancer freelancer = freelancerRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + contractId));

        if (!contract.getFreelancerId().equals(freelancer.getId())) {
            throw new RuntimeException("Unauthorized: contract does not belong to this freelancer");
        }
        if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new IllegalStateException("Only pending contracts can be rejected");
        }

        contract.setStatus(ContractStatus.REJECTED);
        contract.setRejectedAt(LocalDateTime.now());
        contract.setRejectionReason(reason);
        Contract saved = contractRepository.save(contract);

        // Notify the company
        try {
            notificationService.sendContractRejectedNotification(
                    contract.getCompanyId(),
                    contract.getMissionTitle(),
                    contract.getFreelancerName(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send contract rejected notification: {}", e.getMessage());
        }

        return ContractResponse.from(saved);
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    public List<ContractResponse> getFreelancerContracts(String email) {
        Freelancer freelancer = freelancerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        return contractRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId())
                .stream().map(ContractResponse::from).collect(Collectors.toList());
    }

    public List<ContractResponse> getCompanyContracts(String email) {
        Company company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return contractRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId())
                .stream().map(ContractResponse::from).collect(Collectors.toList());
    }

    public ContractResponse getContractById(String contractId, String userEmail) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with id: " + contractId));

        // Verify requester is freelancer or company involved
        boolean isFreelancer = freelancerRepository.findByEmail(userEmail)
                .map(f -> f.getId().equals(contract.getFreelancerId())).orElse(false);
        boolean isCompany = companyRepository.findByEmail(userEmail)
                .map(c -> c.getId().equals(contract.getCompanyId())).orElse(false);

        if (!isFreelancer && !isCompany) {
            throw new RuntimeException("Unauthorized access to contract");
        }

        return ContractResponse.from(contract);
    }

    // ─── File serving ─────────────────────────────────────────────────────────

    public Resource loadContractFile(String fileName) {
        try {
            Path filePath = contractsPath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new ResourceNotFoundException("Contract file not found: " + fileName);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Bad file path: " + fileName, e);
        }
    }

    // ─── Auto-signature image generator ──────────────────────────────────────

    private String generateCompanyAutoSignature(String companyName) throws Exception {
        Color green = new Color(34, 197, 94);

        // Image: company name at top + circular stamp below
        int imgW = 200, imgH = 195;
        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        // Transparent background
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g.fillRect(0, 0, imgW, imgH);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        // ── Company name at top ───────────────────────────────────────────────
        String display = companyName.length() > 22 ? companyName.substring(0, 22) : companyName;
        Font nameFont = new Font("SansSerif", Font.BOLD, 13);
        g.setFont(nameFont);
        g.setColor(new Color(26, 26, 46));
        FontMetrics nfm = g.getFontMetrics();
        g.drawString(display, (imgW - nfm.stringWidth(display)) / 2, 15);

        // ── Stamp centered below name ─────────────────────────────────────────
        int cx = imgW / 2;
        int cy = 20 + (imgH - 20) / 2;   // stamp center Y
        int r  = (imgH - 22) / 2 - 2;    // outer radius

        // Outer circle (thick green)
        g.setColor(green);
        g.setStroke(new BasicStroke(3.5f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);

        // Inner circle (thinner)
        int innerR = r - 12;
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        // ── Curved "APPROVED" at top arc (205° → 335°, through 270°=top) ─────
        Font arcFont = new Font("SansSerif", Font.BOLD, 11);
        int arcR = r - 7;
        drawStampCurvedText(g, "APPROVED", cx, cy, arcR, 205, 335, green, arcFont, true);

        // ── Curved "APPROVED" at bottom arc (inverted, 25° → 155°) ───────────
        // Reverse text so it reads correctly when stamp is flipped
        drawStampCurvedText(g, new StringBuilder("APPROVED").reverse().toString(),
                cx, cy, arcR, 25, 155, green, arcFont, false);

        // ── Green rounded rectangle in center ─────────────────────────────────
        int rectW = innerR * 2 - 10;
        int rectH = 30;
        int rectX = cx - rectW / 2;
        int rectY = cy - rectH / 2;
        g.setColor(green);
        g.fillRoundRect(rectX, rectY, rectW, rectH, 10, 10);

        // "APPROVED" in white inside the rectangle
        Font centerFont = new Font("SansSerif", Font.BOLD, 14);
        g.setFont(centerFont);
        g.setColor(Color.WHITE);
        FontMetrics cfm = g.getFontMetrics();
        String approvedTxt = "APPROVED";
        g.drawString(approvedTxt,
                cx - cfm.stringWidth(approvedTxt) / 2,
                rectY + rectH / 2 + cfm.getAscent() / 2 - 3);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Draws text curved along a circle arc.
     * topArc=true  → characters upright, tops pointing outward (normal reading at top)
     * topArc=false → characters inverted (rubber-stamp style at bottom)
     */
    private void drawStampCurvedText(Graphics2D g, String text, int cx, int cy, int radius,
                                     double startDeg, double endDeg,
                                     Color color, Font font, boolean topArc) {
        FontMetrics fm = g.getFontMetrics(font);
        char[] chars = text.toCharArray();
        double startRad = Math.toRadians(startDeg);
        double endRad   = Math.toRadians(endDeg);
        double step     = (endRad - startRad) / chars.length;

        for (int i = 0; i < chars.length; i++) {
            double angle = startRad + step * i + step / 2.0;
            double px = cx + radius * Math.cos(angle);
            double py = cy + radius * Math.sin(angle);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(px, py);
            // top arc: rotate so char top points away from center
            // bottom arc: same rotation makes chars appear upside-down (stamp style)
            g2.rotate(angle + Math.PI / 2);
            g2.setFont(font);
            g2.setColor(color);
            String ch = String.valueOf(chars[i]);
            g2.drawString(ch, -fm.stringWidth(ch) / 2,
                    topArc ? fm.getAscent() * 2 / 3 : -fm.getDescent());
            g2.dispose();
        }
    }

    // ─── PDF Generation with PDFBox ───────────────────────────────────────────

    private String generateContractPdf(Contract contract, boolean withSignature) throws IOException {
        String fileName = "contract-" + contract.getId() + (withSignature ? "-signed" : "") + ".pdf";
        Path filePath = contractsPath.resolve(fileName);

        // ── Site theme colors (#3793B0 palette) ─────────────────────────────────
        Color PRIMARY     = new Color(55, 147, 176);   // #3793B0 – main teal
        Color DARK_NAVY   = new Color(26, 26, 46);      // #1a1a2e – site dark
        Color TEAL_DARK   = new Color(42, 122, 150);    // #2a7a96 – teal hover
        Color LIGHT_TEAL  = new Color(224, 242, 239);   // #e0f2ef – soft teal bg
        Color LIGHT_SKY   = new Color(235, 248, 255);   // light sky bg
        Color SLATE       = new Color(51, 65, 85);
        Color TEXT_GRAY   = new Color(107, 114, 128);   // #6b7280
        Color GRAY_BORDER = new Color(226, 232, 240);

        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        PDType1Font fontBold  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontReg   = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fontObliq = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        float pageWidth  = page.getMediaBox().getWidth();   // 595
        float pageHeight = page.getMediaBox().getHeight();  // 842
        float margin = 55f;
        float contentWidth = pageWidth - 2 * margin;

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

            // ══════════════════════════════════════════════════════════════════
            // HEADER — Letterhead style
            // ══════════════════════════════════════════════════════════════════

            // Dark thin top stripe
            cs.setNonStrokingColor(DARK_NAVY);
            cs.addRect(0, pageHeight - 10, pageWidth, 10);
            cs.fill();

            // Main teal bar
            cs.setNonStrokingColor(PRIMARY);
            cs.addRect(0, pageHeight - 58, pageWidth, 48);
            cs.fill();

            // Dark thin bottom stripe of header
            cs.setNonStrokingColor(DARK_NAVY);
            cs.addRect(0, pageHeight - 68, pageWidth, 10);
            cs.fill();

            // Decorative overlapping ovals – top right (letterhead-style)
            fillOval(cs, pageWidth - 118, pageHeight - 92, 75, 102, DARK_NAVY);
            fillOval(cs, pageWidth - 72,  pageHeight - 86, 60, 90,  PRIMARY);
            fillOval(cs, pageWidth - 52,  pageHeight - 80, 40, 74,  TEAL_DARK);

            // ── Logo / Brand area ─────────────────────────────────────────────
            boolean logoLoaded = false;
            try {
                ClassPathResource logoResource = new ClassPathResource("images/logo.png");
                if (logoResource.exists()) {
                    byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                    PDImageXObject logoImg = PDImageXObject.createFromByteArray(doc, logoBytes, "logo");
                    cs.drawImage(logoImg, margin, pageHeight - 107, 34, 34);
                    logoLoaded = true;
                }
            } catch (Exception e) {
                log.debug("Logo not available for PDF: {}", e.getMessage());
            }

            float brandX = logoLoaded ? margin + 40 : margin;

            // "Work" teal + "Link" dark navy
            cs.beginText();
            cs.setFont(fontBold, 17);
            cs.setNonStrokingColor(PRIMARY);
            cs.newLineAtOffset(brandX, pageHeight - 98);
            cs.showText("Work");
            cs.endText();

            float workPx = fontBold.getStringWidth("Work") / 1000f * 17f;
            cs.beginText();
            cs.setFont(fontBold, 17);
            cs.setNonStrokingColor(DARK_NAVY);
            cs.newLineAtOffset(brandX + workPx, pageHeight - 98);
            cs.showText("Link");
            cs.endText();

            cs.beginText();
            cs.setFont(fontReg, 7.5f);
            cs.setNonStrokingColor(TEXT_GRAY);
            cs.newLineAtOffset(brandX, pageHeight - 110);
            cs.showText("FREELANCE PLATFORM");
            cs.endText();

            // ── Contract title & reference ────────────────────────────────────
            float y = pageHeight - 133;

            cs.beginText();
            cs.setFont(fontBold, 13);
            cs.setNonStrokingColor(DARK_NAVY);
            cs.newLineAtOffset(margin, y);
            cs.showText("FREELANCE MISSION CONTRACT");
            cs.endText();

            String ref  = "Ref: WL-" + contract.getId().substring(0, 8).toUpperCase();
            String date = "Date: " + LocalDate.now().format(DATE_FMT);
            float refW  = fontReg.getStringWidth(ref)  / 1000f * 9f;
            float dateW = fontReg.getStringWidth(date) / 1000f * 9f;

            cs.beginText();
            cs.setFont(fontReg, 9);
            cs.setNonStrokingColor(PRIMARY);
            cs.newLineAtOffset(pageWidth - margin - refW, y);
            cs.showText(ref);
            cs.endText();

            cs.beginText();
            cs.setFont(fontReg, 9);
            cs.setNonStrokingColor(TEXT_GRAY);
            cs.newLineAtOffset(pageWidth - margin - dateW, y - 13);
            cs.showText(date);
            cs.endText();

            // Teal divider
            y -= 18;
            drawLine(cs, margin, y, pageWidth - margin, PRIMARY);

            // ── MISSION TITLE ─────────────────────────────────────────────────
            y -= 20;
            cs.beginText();
            cs.setFont(fontBold, 12);
            cs.setNonStrokingColor(DARK_NAVY);
            cs.newLineAtOffset(margin, y);
            cs.showText(contract.getMissionTitle());
            cs.endText();

            // ── PARTIES ───────────────────────────────────────────────────────
            y -= 28;
            drawSectionHeader(cs, fontBold, "PARTIES INVOLVED", margin, y, contentWidth, PRIMARY);

            y -= 20;
            float colW = contentWidth / 2 - 8;

            // Employer box (light teal + PRIMARY accent)
            drawInfoBox(cs, fontBold, fontReg, margin, y - 65, colW, 72,
                    "EMPLOYER (CLIENT)", new String[]{
                            contract.getCompanyName(),
                            "Email: " + contract.getCompanyEmail()
                    }, LIGHT_TEAL, PRIMARY);

            // Freelancer box (light sky + TEAL_DARK accent)
            drawInfoBox(cs, fontBold, fontReg, margin + colW + 16, y - 65, colW, 72,
                    "FREELANCER", new String[]{
                            contract.getFreelancerName(),
                            "Email: " + contract.getFreelancerEmail()
                    }, LIGHT_SKY, TEAL_DARK);

            // ── MISSION DETAILS ───────────────────────────────────────────────
            y -= 103;
            drawSectionHeader(cs, fontBold, "MISSION DETAILS", margin, y, contentWidth, PRIMARY);

            y -= 22;
            String startDate = contract.getStartDate() != null ? contract.getStartDate().format(DATE_FMT) : "TBD";
            String endDate   = contract.getEndDate()   != null ? contract.getEndDate().format(DATE_FMT)   : "TBD";
            String salary    = contract.getSalary()    != null
                    ? String.format("%.2f TND/day", contract.getSalary()) : "N/A";

            drawLabelValue(cs, fontBold, fontReg, margin, y, "Mission Title:", contract.getMissionTitle());
            y -= 17; drawLabelValue(cs, fontBold, fontReg, margin, y, "Start Date:",      startDate);
            y -= 17; drawLabelValue(cs, fontBold, fontReg, margin, y, "End Date:",        endDate);
            y -= 17; drawLabelValue(cs, fontBold, fontReg, margin, y, "Daily Rate (TJM):", salary);

            // ── CONTRACT TERMS ────────────────────────────────────────────────
            y -= 30;
            drawSectionHeader(cs, fontBold, "CONTRACT TERMS & CONDITIONS", margin, y, contentWidth, PRIMARY);

            y -= 20;
            String[] termLines = wrapText(contract.getTerms() != null ? contract.getTerms() : "", fontReg, 10, contentWidth);
            for (String line : termLines) {
                if (y < 195) break;
                cs.beginText();
                cs.setFont(fontReg, 10);
                cs.setNonStrokingColor(SLATE);
                cs.newLineAtOffset(margin, y);
                cs.showText(line);
                cs.endText();
                y -= 14;
            }

            // ── SIGNATURES ────────────────────────────────────────────────────
            y = 178;
            drawLine(cs, margin, y + 5, pageWidth - margin, GRAY_BORDER);
            drawSectionHeader(cs, fontBold, "SIGNATURES", margin, y - 8, contentWidth, PRIMARY);

            float sigY = y - 78;
            boolean companySigned = contract.getCompanySignedAt() != null;
            boolean freelancerSigned = contract.getStatus() == ContractStatus.SIGNED;

            drawSignatureBlock(cs, fontBold, fontReg, fontObliq,
                    margin, sigY, colW, "Employer Signature", contract.getCompanyName(),
                    contract.getCompanySignedAt(), companySigned);

            drawSignatureBlock(cs, fontBold, fontReg, fontObliq,
                    margin + colW + 16, sigY, colW,
                    "Freelancer Signature", contract.getFreelancerName(),
                    contract.getSignedAt(), freelancerSigned);

            if (withSignature && freelancerSigned && contract.getSignatureImageBase64() != null) {
                try {
                    String b64 = contract.getSignatureImageBase64();
                    if (b64.contains(",")) b64 = b64.split(",")[1];
                    byte[] imgBytes = Base64.getDecoder().decode(b64);
                    PDImageXObject sigImage = PDImageXObject.createFromByteArray(doc, imgBytes, "freelancer-sig");
                    cs.drawImage(sigImage, margin + colW + 16, sigY + 10, 140, 50);
                } catch (Exception e) {
                    log.warn("Could not embed freelancer signature image: {}", e.getMessage());
                }
            }

            if (withSignature && companySigned && contract.getCompanySignatureImageBase64() != null) {
                try {
                    String b64 = contract.getCompanySignatureImageBase64();
                    if (b64.contains(",")) b64 = b64.split(",")[1];
                    byte[] imgBytes = Base64.getDecoder().decode(b64);
                    PDImageXObject sigImage = PDImageXObject.createFromByteArray(doc, imgBytes, "company-sig");
                    // Display as a square stamp (200x195 → 75x73 in PDF units)
                    cs.drawImage(sigImage, margin + 5, sigY - 10, 75, 73);
                } catch (Exception e) {
                    log.warn("Could not embed company signature image: {}", e.getMessage());
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // FOOTER — Letterhead style (mirrored bottom)
            // ══════════════════════════════════════════════════════════════════

            // Decorative overlapping ovals – bottom left (mirrored from top-right)
            fillOval(cs, 42,  -20, 75, 102, DARK_NAVY);
            fillOval(cs, -2,  -14, 60, 90,  PRIMARY);
            fillOval(cs, -20, -8,  40, 74,  TEAL_DARK);

            // Dark thin bottom bar
            cs.setNonStrokingColor(DARK_NAVY);
            cs.addRect(0, 0, pageWidth, 10);
            cs.fill();

            // Main teal footer bar
            cs.setNonStrokingColor(PRIMARY);
            cs.addRect(0, 10, pageWidth, 42);
            cs.fill();

            // Dark thin top stripe of footer
            cs.setNonStrokingColor(DARK_NAVY);
            cs.addRect(0, 52, pageWidth, 10);
            cs.fill();

            // Footer text
            cs.beginText();
            cs.setFont(fontReg, 8);
            cs.setNonStrokingColor(Color.WHITE);
            cs.newLineAtOffset(margin + 65, 27);
            cs.showText("WorkLink Platform  |  Freelance Mission Contract  |  Status: "
                    + (freelancerSigned ? "SIGNED" : "PENDING SIGNATURE"));
            cs.endText();
        }

        doc.save(filePath.toFile());
        doc.close();

        return "/api/files/contracts/" + fileName;
    }

    // ─── PDF drawing helpers ──────────────────────────────────────────────────

    private void drawLine(PDPageContentStream cs, float x1, float y, float x2, Color color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private void drawSectionHeader(PDPageContentStream cs, PDType1Font font, String text,
                                   float x, float y, float width, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y - 4, width, 18);
        cs.fill();

        cs.beginText();
        cs.setFont(font, 10);
        cs.setNonStrokingColor(Color.WHITE);
        cs.newLineAtOffset(x + 6, y + 2);
        cs.showText(text);
        cs.endText();
    }

    /** Draws a filled oval (ellipse) using Bezier curves. */
    private void fillOval(PDPageContentStream cs, float x, float y, float w, float h, Color color) throws IOException {
        final float k = 0.5522848f; // Bezier circle approximation constant
        float rx = w / 2f, ry = h / 2f;
        float cx = x + rx,  cy = y + ry;
        cs.setNonStrokingColor(color);
        cs.moveTo(cx - rx, cy);
        cs.curveTo(cx - rx, cy + ry * k, cx - rx * k, cy + ry, cx, cy + ry);
        cs.curveTo(cx + rx * k, cy + ry, cx + rx, cy + ry * k, cx + rx, cy);
        cs.curveTo(cx + rx, cy - ry * k, cx + rx * k, cy - ry, cx, cy - ry);
        cs.curveTo(cx - rx * k, cy - ry, cx - rx, cy - ry * k, cx - rx, cy);
        cs.fill();
    }

    private void drawInfoBox(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontReg,
                             float x, float y, float w, float h,
                             String title, String[] lines,
                             Color bgColor, Color headerColor) throws IOException {
        // Background
        cs.setNonStrokingColor(bgColor);
        cs.addRect(x, y, w, h);
        cs.fill();

        // Left border accent
        cs.setNonStrokingColor(headerColor);
        cs.addRect(x, y, 4, h);
        cs.fill();

        // Title
        cs.beginText();
        cs.setFont(fontBold, 8);
        cs.setNonStrokingColor(headerColor);
        cs.newLineAtOffset(x + 10, y + h - 16);
        cs.showText(title);
        cs.endText();

        // Lines
        float lineY = y + h - 30;
        for (String line : lines) {
            cs.beginText();
            cs.setFont(fontReg, 9);
            cs.setNonStrokingColor(new Color(30, 41, 59));
            cs.newLineAtOffset(x + 10, lineY);
            cs.showText(line != null ? line : "");
            cs.endText();
            lineY -= 14;
        }
    }

    private void drawLabelValue(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontReg,
                                float x, float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.setNonStrokingColor(new Color(51, 65, 85));
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(fontReg, 10);
        cs.setNonStrokingColor(new Color(71, 85, 105));
        cs.newLineAtOffset(x + 130, y);
        cs.showText(value != null ? value : "");
        cs.endText();
    }

    private void drawSignatureBlock(PDPageContentStream cs, PDType1Font fontBold,
                                   PDType1Font fontReg, PDType1Font fontObliq,
                                   float x, float y, float w,
                                   String title, String name,
                                   LocalDateTime signedAt, boolean signed) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, 9);
        cs.setNonStrokingColor(new Color(51, 65, 85));
        cs.newLineAtOffset(x, y + 60);
        cs.showText(title + ":");
        cs.endText();

        // Signature box
        cs.setStrokingColor(new Color(148, 163, 184));
        cs.setLineWidth(1f);
        cs.addRect(x, y + 5, w, 50);
        cs.stroke();

        if (signed && signedAt != null) {
            cs.beginText();
            cs.setFont(fontObliq, 8);
            cs.setNonStrokingColor(new Color(22, 163, 74));
            cs.newLineAtOffset(x + 5, y + 30);
            cs.showText("Signed on " + signedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            cs.endText();
        } else if (!signed) {
            cs.beginText();
            cs.setFont(fontObliq, 8);
            cs.setNonStrokingColor(new Color(148, 163, 184));
            cs.newLineAtOffset(x + 5, y + 30);
            cs.showText("Awaiting signature...");
            cs.endText();
        }

        // Name under box
        cs.beginText();
        cs.setFont(fontReg, 9);
        cs.setNonStrokingColor(new Color(71, 85, 105));
        cs.newLineAtOffset(x, y - 5);
        cs.showText(name);
        cs.endText();
    }

    private String[] wrapText(String text, PDType1Font font, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) return new String[0];
        String[] paragraphs = text.split("\n");
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String para : paragraphs) {
            if (para.trim().isEmpty()) { lines.add(""); continue; }
            String[] words = para.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String test = current.length() == 0 ? word : current + " " + word;
                try {
                    float w = font.getStringWidth(test) / 1000 * fontSize;
                    if (w > maxWidth && current.length() > 0) {
                        lines.add(current.toString());
                        current = new StringBuilder(word);
                    } else {
                        current = new StringBuilder(test);
                    }
                } catch (Exception e) {
                    current = new StringBuilder(test);
                }
            }
            if (current.length() > 0) lines.add(current.toString());
        }
        return lines.toArray(new String[0]);
    }

    // ─── Default contract terms ───────────────────────────────────────────────

    private String buildDefaultTerms(Mission mission, Freelancer freelancer, Company company) {
        String startDate = mission.getStartDate() != null ? mission.getStartDate().format(DATE_FMT) : "TBD";
        String endDate   = mission.getEndDate()   != null ? mission.getEndDate().format(DATE_FMT)   : "TBD";
        double tjm       = mission.getTjm() != null ? mission.getTjm() : 0;

        return "1. ENGAGEMENT\n" +
               "The Employer (" + company.getCompanyName() + ") engages the Freelancer (" +
               freelancer.getFirstName() + " " + freelancer.getLastName() + ") for the mission " +
               "\"" + mission.getJobTitle() + "\" for the duration from " + startDate + " to " + endDate + ".\n\n" +
               "2. REMUNERATION\n" +
               "The Freelancer shall be remunerated at a daily rate (TJM) of " +
               String.format("%.2f TND", tjm) + " per working day, payable monthly upon receipt of invoice.\n\n" +
               "3. DELIVERABLES & OBLIGATIONS\n" +
               "The Freelancer agrees to deliver services as agreed upon during this mission. " +
               "The Freelancer shall maintain confidentiality regarding all proprietary information " +
               "obtained during the performance of this contract.\n\n" +
               "4. INTELLECTUAL PROPERTY\n" +
               "All work products, deliverables, and intellectual property created during this mission " +
               "shall remain the property of the Employer upon full payment.\n\n" +
               "5. TERMINATION\n" +
               "Either party may terminate this contract with 14 days written notice. " +
               "In case of breach, the non-breaching party may terminate immediately.\n\n" +
               "6. GOVERNING LAW\n" +
               "This contract is governed by the laws of Tunisia. Any disputes shall be resolved " +
               "through the competent courts of Tunisia.\n\n" +
               "By signing below, both parties agree to the terms and conditions set forth in this contract.";
    }
}
