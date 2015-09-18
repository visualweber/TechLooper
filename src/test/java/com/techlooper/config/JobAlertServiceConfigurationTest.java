package com.techlooper.config;

import com.techlooper.service.JobAggregatorService;
import com.techlooper.service.impl.JobAggregatorServiceImpl;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by NguyenDangKhoa on 7/13/15.
 */
@Configuration
@PropertySources({@PropertySource("classpath:techlooper.properties")})
public class JobAlertServiceConfigurationTest {

    @Value("${mail.techlooper.form}")
    private String mailTechlooperForm;

    @Value("${mail.techlooper.reply_to}")
    private String mailTechlooperReplyTo;

    @Value("classpath:template/jobAlert.en.ftl")
    private Resource jobAlertEmailTemplate;

    @Value("classpath:topPriorityJobId.csv")
    private org.springframework.core.io.Resource topPriorityJobIdResource;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

//    @Bean
//    public JobAggregatorService jobAlertService() {
//        return new JobAggregatorServiceImpl();
//    }

    @Bean
    public Mapper dozerBeanMapper() {
        return new DozerBeanMapper();
    }

    @Bean
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(25);

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.transport.protocol", "smtp");
        mailSender.setJavaMailProperties(javaMailProperties);
        return mailSender;
    }

    @Bean
    public freemarker.template.Configuration freemakerConfig() throws IOException, URISyntaxException {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_22);
        cfg.setDirectoryForTemplateLoading(jobAlertEmailTemplate.getFile().getParentFile());
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return cfg;
    }

    @Bean
    public MimeMessage jobAlertMailMessage(JavaMailSender mailSender) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mailMessage = mailSender.createMimeMessage();
        mailMessage.setReplyTo(InternetAddress.parse(mailTechlooperReplyTo));
        mailMessage.setFrom(new InternetAddress(mailTechlooperForm, "TechLooper", "UTF-8"));
        return mailMessage;
    }

    @Bean
    public Template jobAlertMailTemplateEn(freemarker.template.Configuration freemakerConfig) throws IOException {
        Template template = freemakerConfig.getTemplate("jobAlert.en.ftl");
        return template;
    }

    @Bean
    public Template jobAlertMailTemplateVi(freemarker.template.Configuration freemakerConfig) throws IOException {
        Template template = freemakerConfig.getTemplate("jobAlert.vi.ftl");
        return template;
    }

    @Bean
    public Set<String> topPriorityJobIds() throws IOException {
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(new FileReader(topPriorityJobIdResource.getFile()));
        Set<String> ids = new HashSet<>();
        for (CSVRecord record : records) {
            ids.add(record.get(0));
        }
        return ids;
    }

}
