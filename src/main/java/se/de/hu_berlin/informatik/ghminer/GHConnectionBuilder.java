package se.de.hu_berlin.informatik.ghminer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.kohsuke.github.GHAuthorization;
import org.kohsuke.github.GitHub;

import se.de.hu_berlin.informatik.utils.miscellaneous.Log;

/**
 * Establishes the connection to the git hub
 */
public class GHConnectionBuilder {

	// this token is used when the user has no token and does not want to
	// provide
	// or has no credentials
	private final static String DEFAULT_TOKEN = "f2bdb434cf85127eb7ed3210f467747bac625ec2";
	private final static String PROP_FILE_NAME = ".github";
	private final static String PROP_KEY_TOKEN = "oauth";
	private final static String TOKEN_NAME = "GitHubMiner_Token";

	/**
	 * Tries to get a connection to git hub. If the user already has an auth
	 * token in his .github file or system environment it is used. Otherwise the
	 * user name and password will be used to first get a basic authentication
	 * (clear text) and create a token afterwards. If the user has no
	 * credentials a default token is used which may result in conflicting rate
	 * limit errors.
	 * 
	 * @param aUser
	 *            The name of the user or null
	 * @param aPwd
	 *            The password of the user or null
	 * @return An authenticated git hub object
	 */
	public static GitHub getConnection(String aUser, String aPwd) {
		try {
			// check if a token already exists
			return GitHub.connect();
		} catch (IOException e) {
			// the normal connect did not work
			Log.out(GHConnectionBuilder.class, "No auth token could be found.");
			return createToken(aUser, aPwd);
		}
	}

	/**
	 * If a user name and password are provided this method will try to create a
	 * token, save it in a .github properties file and connect to git hub using
	 * it. Otherwise the default token is used.
	 * 
	 * @param aUser
	 *            The name of the user
	 * @param aPwd
	 *            The password of the user
	 * @return A git hub object
	 */
	private static GitHub createToken(String aUser, String aPwd) {
		GitHub gh = null;
		String token = "";

		try {
			if (aUser != null && aPwd != null) {
				Log.out(GHConnectionBuilder.class, "Creating a token using basic authentication");
				gh = GitHub.connectUsingPassword(aUser, aPwd);

				GHAuthorization gha = null;

				try {
					gha = gh.createToken(null, TOKEN_NAME, null);
					token = gha.getToken();
				} catch (IOException e) {
					Log.out(GHConnectionBuilder.class, "Aborting the token generation...  Using the default (shared) token...", e);
					Log.out(GHConnectionBuilder.class, "Please delete the auth token " + TOKEN_NAME + " from your profile");
					token = DEFAULT_TOKEN;
				}

				updateGitHubProps(token);

			} else {
				Log.out(GHConnectionBuilder.class, "Using the default (shared) auth token because no credentials were provided. Download rate may be slower.");
				return GitHub.connectUsingOAuth(DEFAULT_TOKEN);
			}

			gh = GitHub.connectUsingOAuth(token);
		} catch (IOException e) {
			Log.out(GHConnectionBuilder.class, "Could not build a connection", e);
		}

		return gh;
	}

	/**
	 * Updates the .github properties files with a new token. If this file does
	 * not exist it will be generated.
	 * 
	 * @param aToken
	 *            The oauth token
	 */
	private static void updateGitHubProps(String aToken) {
		File homeDir = new File(System.getProperty("user.home"));
		File propertyFile = new File(homeDir, PROP_FILE_NAME);

		Properties props = new Properties();

		if (propertyFile.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(propertyFile);
				props.load(fis);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException e) {
					// nothing to do
				}
			}
		}

		// update the token property which also happens to be the only property
		props.setProperty(PROP_KEY_TOKEN, aToken);

		// write the updated properties file to the file system
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(propertyFile);
			props.store(fos, "The properties files for git hub authentication tokens");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// nothing to do
				}
			}
		}

		Log.out(GHConnectionBuilder.class, "Updated the token properties file at: " + propertyFile.getAbsolutePath());
	}

}
