/*
 * Copyright (C) 2007 Luca Veltri - University of Parma - Italy
 * 
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package org.mjsip.ua;



import java.io.File;

import org.mjsip.media.MediaDesc;
import org.mjsip.sip.address.GenericURI;
import org.mjsip.sip.address.NameAddress;
import org.mjsip.sip.provider.SipProvider;
import org.mjsip.sip.provider.SipStack;
import org.slf4j.LoggerFactory;



/**
 * {@link AnsweringMachine} is a VOIP server that automatically accepts incoming calls, sends an audio file and records
 * input received from the remote end.
 */
public class AnsweringMachine extends MultipleUAS {
	
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AnsweringMachine.class);

	/** Default available ports */
	public static int MEDIA_PORTS=20;

	/** Maximum life time (call duration) in seconds */
	public static int MAX_LIFE_TIME=600;

	/** Media file to play when answering the call. */
	public static String ANNOUNCEMENT_FILE="./announcement-8000hz-mono-a-law.wav";

	/** First media port */
	int _firstMediaPort;

	/** Last media port */
	int _lastMediaPort;


	/** Creates an {@link AnsweringMachine}. */
	public AnsweringMachine(SipProvider sip_provider, UserAgentProfile ua_profile, int numberOfPorts) {
		super(sip_provider,ua_profile);

		_firstMediaPort = ua_profile.getMediaPort();
		_lastMediaPort = _firstMediaPort + numberOfPorts - 1;
	} 


	/** From UserAgentListener. When a new call is incoming. */
	@Override
	public void onUaIncomingCall(UserAgent ua, NameAddress callee, NameAddress caller, MediaDesc[] media_descs) {
		GenericURI address = callee.getAddress();
		LOG.info("Incomming call from: " + address);

		String audioFile = ANNOUNCEMENT_FILE;
		if (new File(audioFile).isFile()) {
			LOG.info("Playing media: " + audioFile);

			ua_profile.send_file = audioFile;

			int current_media_port = ua_profile.getMediaPort();
			if ((current_media_port += media_descs.length) > _lastMediaPort) {
				current_media_port = _firstMediaPort;
			}
			ua_profile.setMediaPort(current_media_port, 1);

			ua.accept();
		} else {
			LOG.info("Media not found, rejecting call: " + audioFile);
			ua.hangup();
		}
	}
	

	/** The main method. */
	public static void main(String[] args) {
		String program = AnsweringMachine.class.getSimpleName();
		LOG.info(program + " " + SipStack.version);

		int media_ports=MEDIA_PORTS;
		boolean prompt_exit=false;

		for (int i=0; i<args.length; i++) {
			if (args[i].equals("--mports")) {
				try {
					media_ports=Integer.parseInt(args[i+1]);
					args[i]="--skip";
					args[++i]="--skip";
				}
				catch (Exception e) {  e.printStackTrace();  }
			}
			else
			if (args[i].equals("--announcement")) {
				ANNOUNCEMENT_FILE=args[i+1];
				args[i]="--skip";
				args[++i]="--skip";
			}
			else
			if (args[i].equals("--prompt")) {
				prompt_exit=true;
				args[i]="--skip";
			}
		}
		
		
		UserAgentConfig config = UserAgentConfig.init(program, args);
		config.ua_profile.audio = true;
		config.ua_profile.video = false;
		config.ua_profile.send_only = true;
		if (config.ua_profile.hangup_time <= 0)
			config.ua_profile.hangup_time = MAX_LIFE_TIME;
		new AnsweringMachine(config.sip_provider, config.ua_profile, media_ports);
		
		// promt before exit
		if (prompt_exit) 
		try {
			System.out.println("press 'enter' to exit");
			(new java.io.BufferedReader(new java.io.InputStreamReader(System.in))).readLine();
			System.exit(0);
		}
		catch (Exception e) {}
	}    

}