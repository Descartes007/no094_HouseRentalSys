package org.example.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import org.example.springboot.entity.House;
import org.example.springboot.entity.LeaseRecord;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class ContractGenerator {

    private static final String BASE_PATH = "files/contract/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    static {
        // 确保存储合同的目录存在
        File contractDir = new File(BASE_PATH);
        if (!contractDir.exists()) {
            contractDir.mkdirs();
        }
    }

    /**
     * 生成租房合同PDF文件
     * @param leaseRecord 租赁记录
     * @param order 订单信息
     * @param house 房屋信息
     * @param tenant 租客信息
     * @param landlord 房东信息
     * @return 生成的合同文件路径
     */
    public String generateContract(LeaseRecord leaseRecord, Order order, House house, User tenant, User landlord) {
        String fileName = "contract_" + UUID.randomUUID().toString().replace("-", "") + ".pdf";
        String filePath = BASE_PATH + fileName;
        
        try (PDDocument document = new PDDocument()) {
            // 创建页面
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // 使用标准字体 - Helvetica

            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // 尝试加载中文字体 (从资源路径)
            PDFont chineseFont = null;
            try {
                // 尝试使用内置的中文字体（STSong-Light是iText内置的支持中文的字体）
                ClassPathResource resource = new ClassPathResource("fonts/simfang.ttf");
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        chineseFont = PDType0Font.load(document, is);
                        log.info("成功加载中文字体");
                    }
                } else {
                    log.warn("中文字体文件不存在，将使用替代方案");
                }
            } catch (Exception e) {
                log.warn("无法加载中文字体: {}", e.getMessage());
            }
            
            // 创建内容流
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;
            float startY = page.getMediaBox().getHeight() - margin;
            float fontSize = 12;
            float leading = 1.5f * fontSize;
            
            // 绘制标题 - 根据是否有中文字体来决定使用何种标题
            if (chineseFont != null) {
                // 使用中文字体绘制中文标题
                float titleWidth = chineseFont.getStringWidth("房屋租赁合同") / 1000 * 18;
                contentStream.beginText();
                contentStream.setFont(chineseFont, 18);
                contentStream.newLineAtOffset((page.getMediaBox().getWidth() - titleWidth) / 2, startY);
                contentStream.showText("房屋租赁合同");
                contentStream.endText();
            } else {
                // 使用西文字体绘制英文标题
                float titleWidth = fontBold.getStringWidth("HOUSING RENTAL CONTRACT") / 1000 * 18;
                contentStream.beginText();
                contentStream.setFont(fontBold, 18);
                contentStream.newLineAtOffset((page.getMediaBox().getWidth() - titleWidth) / 2, startY);
                contentStream.showText("HOUSING RENTAL CONTRACT");
                contentStream.endText();
            }
            
            // 绘制合同编号
            contentStream.beginText();
            contentStream.setFont(fontNormal, fontSize);
            contentStream.newLineAtOffset(margin, startY - 40);
            if (chineseFont != null) {
                contentStream.setFont(chineseFont, fontSize);
                contentStream.showText("合同编号：" + order.getOrderNo());
            } else {
                contentStream.showText("Contract No.: " + order.getOrderNo());
            }
            contentStream.endText();
            
            // 绘制甲乙方信息
            float currentY = startY - 80;
            
            // 使用适当的字体绘制文本
            PDFont textFont = (chineseFont != null) ? chineseFont : fontNormal;
            PDFont titleFont = (chineseFont != null) ? chineseFont : fontBold;
            
            // 甲方信息
            contentStream.beginText();
            contentStream.setFont(titleFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("甲方(出租方)：" + landlord.getName());
            } else {
                contentStream.showText("Party A (Landlord): " + landlord.getName());
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("身份证号：" + (landlord.getIdCard() != null ? landlord.getIdCard() : "无"));
            } else {
                contentStream.showText("ID: " + (landlord.getIdCard() != null ? landlord.getIdCard() : "Not provided"));
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("联系电话：" + landlord.getPhone());
            } else {
                contentStream.showText("Phone: " + landlord.getPhone());
            }
            contentStream.endText();
            
            currentY -= leading * 1.5f;
            
            // 乙方信息
            contentStream.beginText();
            contentStream.setFont(titleFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("乙方(承租方)：" + tenant.getName());
            } else {
                contentStream.showText("Party B (Tenant): " + tenant.getName());
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("身份证号：" + (tenant.getIdCard() != null ? tenant.getIdCard() : "无"));
            } else {
                contentStream.showText("ID: " + (tenant.getIdCard() != null ? tenant.getIdCard() : "Not provided"));
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("联系电话：" + tenant.getPhone());
            } else {
                contentStream.showText("Phone: " + tenant.getPhone());
            }
            contentStream.endText();
            
            currentY -= leading * 2;
            
            // 第一条
            contentStream.beginText();
            contentStream.setFont(titleFont, 14);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("第一条：房屋基本情况");
            } else {
                contentStream.showText("Article 1: Basic Information of Housing");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("1.1 房屋坐落于：" + house.getAddress());
            } else {
                contentStream.showText("1.1 Address: " + house.getAddress());
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("1.2 房屋面积：" + house.getArea() + " 平方米");
            } else {
                contentStream.showText("1.2 Area: " + house.getArea() + " square meters");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("1.3 房屋类型：" + house.getTitle());
            } else {
                contentStream.showText("1.3 Type: " + house.getTitle());
            }
            contentStream.endText();
            
            currentY -= leading * 1.5f;
            
            // 第二条
            contentStream.beginText();
            contentStream.setFont(titleFont, 14);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("第二条：租赁期限");
            } else {
                contentStream.showText("Article 2: Lease Term");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            String startDate = formatDateForPdf(leaseRecord.getStartDate());
            String endDate = formatDateForPdf(leaseRecord.getEndDate());
            int months = calculateMonths(leaseRecord.getStartDate(), leaseRecord.getEndDate());
            if (chineseFont != null) {
                contentStream.showText("2.1 租赁期自 " + startDate + " 起至 " + endDate + " 止，共计 " + months + " 个月。");
            } else {
                contentStream.showText("2.1 From " + startDate + " to " + endDate + ", total " + months + " months.");
            }
            contentStream.endText();
            
            currentY -= leading * 1.5f;
            
            // 第三条
            contentStream.beginText();
            contentStream.setFont(titleFont, 14);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("第三条：租金及支付方式");
            } else {
                contentStream.showText("Article 3: Rent and Payment");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("3.1 月租金：人民币 " + house.getPrice() + " 元整");
            } else {
                contentStream.showText("3.1 Monthly Rent: RMB " + house.getPrice());
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("3.2 支付方式：" + getPaymentCycleText(leaseRecord.getPaymentCycle()));
            } else {
                contentStream.showText("3.2 Payment Method: " + getEnglishPaymentCycleText(leaseRecord.getPaymentCycle()));
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("3.3 押金：人民币 " + order.getDeposit() + " 元整");
            } else {
                contentStream.showText("3.3 Deposit: RMB " + order.getDeposit());
            }
            contentStream.endText();
            
            currentY -= leading * 1.5f;
            
            // 第四条
            contentStream.beginText();
            contentStream.setFont(titleFont, 14);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("第四条：甲方义务");
            } else {
                contentStream.showText("Article 4: Obligations of Party A");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("4.1 按约定交付房屋及设施");
            } else {
                contentStream.showText("4.1 Deliver the house and facilities to Party B as agreed.");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("4.2 负责对房屋进行维修养护");
            } else {
                contentStream.showText("4.2 Responsible for house maintenance.");
            }
            contentStream.endText();
            
            currentY -= leading * 1.5f;
            
            // 第五条
            contentStream.beginText();
            contentStream.setFont(titleFont, 14);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("第五条：乙方义务");
            } else {
                contentStream.showText("Article 5: Obligations of Party B");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("5.1 按时支付租金");
            } else {
                contentStream.showText("5.1 Pay rent on time.");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("5.2 正确使用并维护房屋");
            } else {
                contentStream.showText("5.2 Properly use and maintain the house.");
            }
            contentStream.endText();
            
            currentY -= leading;
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("5.3 未经甲方同意，不得转租");
            } else {
                contentStream.showText("5.3 Not to sublease without Party A's consent.");
            }
            contentStream.endText();
            
            currentY -= leading * 2;
            
            // 签名区域
            contentStream.beginText();
            contentStream.setFont(titleFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("签名区域：");
            } else {
                contentStream.showText("Signatures:");
            }
            contentStream.endText();
            
            currentY -= leading * 1.5f;
            
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("甲方(出租方)： _________________");
            } else {
                contentStream.showText("Party A (Landlord): _________________");
            }
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(width / 2 + margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("日期： _________________");
            } else {
                contentStream.showText("Date: _________________");
            }
            contentStream.endText();
            
            currentY -= leading * 2;
            
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("乙方(承租方)： _________________");
            } else {
                contentStream.showText("Party B (Tenant): _________________");
            }
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(textFont, fontSize);
            contentStream.newLineAtOffset(width / 2 + margin, currentY);
            if (chineseFont != null) {
                contentStream.showText("日期： _________________");
            } else {
                contentStream.showText("Date: _________________");
            }
            contentStream.endText();
            
            // 关闭内容流
            contentStream.close();
            
            // 保存文档
            document.save(filePath);
            
            log.info("合同生成成功：{}", filePath);
            return "/contract/" + fileName; // 返回相对路径
        } catch (IOException e) {
            log.error("生成合同PDF文件失败", e);
            return null;
        }
    }
    
    /**
     * 将日期格式化为PDF合同中使用的格式
     */
    private String formatDateForPdf(LocalDate date) {
        if (date == null) {
            return "未知";
        }
        return date.format(DATE_FORMATTER);
    }
    
    /**
     * 计算两个日期之间的月数
     */
    private int calculateMonths(LocalDate startDate, LocalDate endDate) {
        return endDate.getMonthValue() - startDate.getMonthValue() + 
               (endDate.getYear() - startDate.getYear()) * 12;
    }
    
    /**
     * 根据支付周期代码返回中文描述
     */
    private String getPaymentCycleText(String paymentCycle) {
        switch (paymentCycle) {
            case "MONTHLY":
                return "月付";
            case "QUARTERLY":
                return "季付";
            case "YEARLY":
                return "年付";
            default:
                return "月付";
        }
    }
    
    /**
     * 根据支付周期代码返回英文描述
     */
    private String getEnglishPaymentCycleText(String paymentCycle) {
        switch (paymentCycle) {
            case "MONTHLY":
                return "Monthly";
            case "QUARTERLY":
                return "Quarterly";
            case "YEARLY":
                return "Yearly";
            default:
                return "Monthly";
        }
    }
} 