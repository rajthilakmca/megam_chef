/* 
 ** Copyright [2012-2013] [Megam Systems]
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 ** http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
package org.megam.chef.core;

import java.io.IOException;
import java.util.List;

import org.megam.chef.AppYaml;
import org.megam.chef.BootStrapChef;
import org.megam.chef.exception.IdentifierException;
import org.megam.chef.exception.ProvisionerException;
import org.megam.chef.exception.ShellException;
import org.megam.chef.identity.IIDentity;
import org.megam.chef.identity.IdentityParser;
import org.megam.chef.identity.S3;
import org.megam.chef.parser.JSONRequest;
import org.megam.chef.parser.JSONRequestParser;
import org.megam.chef.shell.Command;
import org.megam.chef.shell.ShellProvisioningPool;
import org.megam.chef.shell.Shellable;
import org.megam.chef.shell.Stoppable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ram
 * @param <T>
 * 
 */

public class DefaultProvisioningServiceWithShell<T> extends
		DefaultProvisioningService<T> implements Shellable, Stoppable {

	private Logger logger = LoggerFactory
			.getLogger(DefaultProvisioningServiceWithShell.class);

	private String vaultLocation;
	
	/**
	 * 
	 * @throws ProvisionerException
	 */
	public DefaultProvisioningServiceWithShell() throws ProvisionerException {
		super();
	}

	/**
	 * TO-DO : What is the output we need to send ? We need a generic way to
	 * convert a Java output to JSON output
	 * 
	 * @throws IdentifierException
	 * @throws IOException
	 * 
	 * @see org.megam.chef.ProvisioningService#provision()
	 */
	@Override
	public T provision(String jsonString) throws ProvisionerException,
			IOException, IdentifierException {
		logger.debug("-------> Entry");
		logger.debug("-------> jsonString =>" + jsonString);
		try {
			execute(jsonToCommand(jsonString));
		} catch (ShellException she) {
			throw new ProvisionerException(she);
		}
		logger.debug("-------> Exit");
		/**
		 * TO-DO why do we return null here ?
		 */
		return null;
	}

	/**
	 * @param args
	 * @throws ShellException
	 * @throws IdentifierException
	 * @throws IOException
	 * @throws ProvisionerException
	 */
	public Command jsonToCommand(String jsonRequest) throws ShellException,
			IOException, IdentifierException, ProvisionerException {
		logger.debug("-------> Entry");
		logger.debug("-------> jsonRequest =>" + jsonRequest);
		Command com = new org.megam.chef.shell.Command(
				convertInput(jsonRequest));
		logger.debug("Exit");
		return com;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.megam.shell.Stoppable#stop()
	 */
	@Override
	public void halt() {
		throw new RuntimeException("Halt not implemented yet.");
	}

	/**
	 * Using GSON library, an input JSON string is parsed to a
	 * GlobalConfiguration Java Object. If the JSON parameters passed are
	 * validated for 1..... 2. ..... 3. .... If they are valid then the shell
	 * builder builds a script. If not an error with the reasons of validation
	 * failure is retured. command
	 * 
	 * @param myJSONString
	 *            logger.debug("-------> Entry");
	 * 
	 * @return
	 * @throws IOException
	 * @throws IdentifierException
	 * @throws ProvisionerException
	 */
	private String[] convertInput(String jsonRequest) throws ShellException,
			IOException, IdentifierException, ProvisionerException {
		logger.debug("-------> Entry");
		logger.debug("-------> jsonRequest =>" + jsonRequest);
		JSONRequestParser jrp = new JSONRequestParser(jsonRequest);
		JSONRequest jr = jrp.data();
		logger.debug("-------> jr =>" + jr);
		/**
		 * Download the stuff from S3 The location to download can be got from
		 * parsing vault_location (in access) S3.download() If all is well
		 * proceed Wrap this method and trap for ProvisionerException
		 */		
		ParmsValidator pv = new ParmsValidator();
		if (pv.validate(jr.conditionList())) {	
			vaultLocation = vaultLocationParser(jr.getAccess().getVaultLocation());
			S3.download(vaultLocation);
			List<IIDentity> fp = new IdentityParser(vaultLocation).identity();
			System.out.println("========================^^^^^^^^^^+++++++" + fp);
			logger.debug("-------> Shellbuilder =>");
			return ShellBuilder.buildString(jr.scriptFeeder(), jrp, fp);
		} else {
			throw new ShellException(new IllegalArgumentException(pv
					.reasonsNotSatisfied().toString()));
		}
	}

	public String vaultLocationParser(String str) {		
	 // str = "https://s3-ap-southeast-1.amazonaws.com/cloudkeys/a@b.com/default";
		int lst=str.lastIndexOf("/");
		String cc = str.substring(lst);
		str=str.replace(str.substring(lst),"");				
		String email = str.substring(str.lastIndexOf("/")+1);
		return email+cc;
	}
	
	public String toString() {
		return "DefaultProvisioningWithShell";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.megam.chef.shell.Shellable#execute(org.megam.chef.shell.Command)
	 */
	@Override
	public void execute(Command command) throws ShellException {
		(new ShellProvisioningPool()).run(command);
	}

}
