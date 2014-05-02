package com.karavel.tutorial.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.tools.ant.taskdefs.condition.HasMethod;
import org.junit.Test;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.karavel.commons.mail.SendMailInterface;
import com.karavel.commons.mail.SendMessageIn;
import com.karavel.commons.mail.address.MailAddress;
import com.karavel.commons.mail.content.Content;
import com.karavel.commons.mail.content.FreemarkerTemplateContentStrategyImpl;
import com.karavel.commons.mail.content.StaticContentStrategyImpl;

public class MailSender {

	public void sendEmail(HashMap<String, Object> map)
			throws InterruptedException {
		HashMap<String, Object> mapFreemarkerLike=new HashMap<String, Object>();
		Set<String> keys = map.keySet();
		for (String key : keys) {
			String newKey=StringUtils.replace(key, ".", "_");
			mapFreemarkerLike.put(newKey, map.get(key));
		}
		
		String recipienders = (String) configProperties
				.get("recipienders.email.addresses");
		String[] recipiendersTab = StringUtils.tokenizeToStringArray(recipienders,
				EMAIL_SEPARATOR);
		MailAddress[] mailAdresseses = new MailAddress[recipiendersTab.length];
		for (int i = 0; i < recipiendersTab.length; i++) {
			mailAdresseses[i] = new MailAddress(recipiendersTab[i]);
		}
		sendMailInterface
				.sendMessage(
				new SendMessageIn(new MailAddress((String) configProperties
						.get("sender.email.address"), (String) configProperties
						.get("sender.email.alias")), mailAdresseses, null,
						null, new Content(new StaticContentStrategyImpl(
								(String)map.get("mail.subject")),
								new FreemarkerTemplateContentStrategyImpl(
										"mailTemplate.plaintext.ftl",
										mapFreemarkerLike, freemarkerConfigurer
												.getConfiguration()), null),
						null, null, "FR", "UTF8",
						new SmtpHeaderWelcomeNewMemberStrategy("BVILLAUMIE",
								"PROMOVACANCES")));
	}

	
	private final static Map<String, Object> TEMPLATE_MODEL = new HashMap<String, Object>();
	static {
		TEMPLATE_MODEL.put("userName", "Dupont");
		TEMPLATE_MODEL.put("emailAddress", "dupont@test.fr");
	}
	private final String EMAIL_SEPARATOR = ", ";
	private Properties configProperties;
	private SendMailInterface sendMailInterface;

	private FreeMarkerConfigurer freemarkerConfigurer;

	public FreeMarkerConfigurer getFreemarkerConfigurer() {
		return freemarkerConfigurer;
	}

	public void setFreemarkerConfigurer(
			FreeMarkerConfigurer freemarkerConfigurer) {
		this.freemarkerConfigurer = freemarkerConfigurer;
	}

	public SendMailInterface getSendMailInterface() {
		return sendMailInterface;
	}

	public void setSendMailInterface(SendMailInterface sendMailInterface) {
		this.sendMailInterface = sendMailInterface;
	}

	public Properties getConfigProperties() {
		return configProperties;
	}

	public void setConfigProperties(Properties configProperties) {
		this.configProperties = configProperties;
	}

}
