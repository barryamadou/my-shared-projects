package com.karavel.tutorial.conf;

import com.karavel.commons.mail.header.SmtpHeaderOverrideStrategy;
import com.karavel.commons.mail.header.SmtpHeaderOverrideStrategyGenericImpl;

/**
 * Sample strategy that overrides smtp header for a dedicated mail 
 * @author bvillaumie
 *
 */
public class SmtpHeaderWelcomeNewMemberStrategy extends SmtpHeaderOverrideStrategyGenericImpl implements SmtpHeaderOverrideStrategy {
	
	private SmtpHeaderWelcomeNewMemberStrategy() {
		super("MY_ARTIFACT_ID");
	}

	public SmtpHeaderWelcomeNewMemberStrategy(String numClient, String marque) {
		this();
		this.properties.setProperty(PROP_NAME_NUMCLIENT, numClient);
		this.properties.setProperty(PROP_NAME_MARQUE, marque);
	}

	@Override
	public String doGetMessageID() {
		return this.properties.getProperty(PROP_NAME_NUMCLIENT);
	}

	private final static String PROP_NAME_NUMCLIENT = "numclient";
	private final static String PROP_NAME_MARQUE = "marque";

}
