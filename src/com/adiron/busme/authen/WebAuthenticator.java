package com.adiron.busme.authen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.os.Binder;
import android.util.Log;

public class WebAuthenticator implements LoginAuthenticator {
    private static final String LOGTAG = WebAuthenticator.class.getName();

    private static final String PARAM_CSRF_TOKEN_FMT = "%s";
    private static final String PARAM_MASTER_FMT = "muni_admin[%s]";
	private static final String PARAM_LOGIN = "muni_admin[email]";
	private static final String PARAM_PASSWORD = "muni_admin[password]";
	
	private BasicHttpContext localHttpContext;
	private DefaultHttpClient httpClient;

	private String csrfParam;

	private String csrfToken;

	private String masterParam;

	private String masterToken;

	private CookieStore cookieStore;

	WebAuthenticator() {
		initializeHttpClient();
	}
	
	private HttpClient initializeHttpClient() {
		  DefaultHttpClient httpclient = new DefaultHttpClient();
		  BasicHttpContext localContext = new BasicHttpContext();
		  cookieStore = httpclient.getCookieStore();
		  localHttpContext = localContext;
		  return httpClient = httpclient;
	}
	
	private String convertStreamToStream(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			line = reader.readLine();
			while(line != null) {
				sb.append(line);
				line = reader.readLine();
				if (line != null) {
					sb.append("\n");
				}
			}
		} finally {
			reader.close();
		}
		Log.d(LOGTAG, "Got " + sb);
		return sb.toString();
	}
	
	private void getJsonTokens(String url) throws ClientProtocolException, IOException {
		HttpGet request = new HttpGet(url);
		HttpResponse resp = httpClient.execute(request, localHttpContext);
		Header[] headers = resp.getAllHeaders();
		for (Header h : headers) {
			Log.d(LOGTAG, "Header: " + h.getName() + " " + h.getValue());
		}
		HttpEntity ent = resp.getEntity();
		InputStream in = ent.getContent();
		try {
			JSONTokener tokener = new JSONTokener(convertStreamToStream(in));
			JSONObject json = new JSONObject(tokener);
//			csrfParam = json.getString("csrf-param");
//			csrfToken = json.getString("csrf-token");
			masterParam = json.getString("master-param");
			masterToken = json.getString("master-token");
		} catch (JSONException e) {
			Log.d(LOGTAG, e.getMessage());
		}
		ent.consumeContent();
//		if (csrfParam == null || csrfToken == null) {
//			throw new IOException("No CSRF Token");
//		}
		if (masterParam == null || masterToken == null) {
			throw new IOException("No Master Token");
		}
	}

	private void getMetaTokens(String url) throws ClientProtocolException, IOException, XmlPullParserException {
		HttpGet request = new HttpGet(url);
		HttpResponse resp = httpClient.execute(request, localHttpContext);
		Header[] headers = resp.getAllHeaders();
		for (Header h : headers) {
			Log.d(LOGTAG, "Header: " + h.getName() + " " + h.getValue());
		}
		HttpEntity ent = resp.getEntity();
		InputStream in = ent.getContent();
		XmlPullParserFactory parserF = XmlPullParserFactory.newInstance();
		XmlPullParser parser = parserF.newPullParser();
		parser.setInput(new InputStreamReader(in));
		//parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
		parser.next();
		boolean inHead = false;
		while(parser.getEventType() != XmlPullParser.END_DOCUMENT && 
				(csrfParam == null || csrfToken == null || masterParam == null || masterToken == null)) {
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				if ("head".equalsIgnoreCase(parser.getName())) {
					inHead = true;
				} else if (inHead &&  "meta".equalsIgnoreCase(parser.getName())) {
					String name = parser.getAttributeValue(null, "name");
//					if ("csrf-param".equals(name)) {
//						csrfParam = parser.getAttributeValue(null, "content");
//					} else if ("csrf-token".equals(name)) {
//						csrfToken = parser.getAttributeValue(null, "content");
//					} else 
					if ("master-param".equals(name)) {
						masterParam = parser.getAttributeValue(null, "content");
					} else if ("master-token".equals(name)) {
						masterToken = parser.getAttributeValue(null, "content");
					}
				}
			} else if (parser.getEventType() == XmlPullParser.END_TAG) {
				if (inHead && "head".equalsIgnoreCase(parser.getName())) {
					break;
				}
			}
			parser.next();
		}
		ent.consumeContent();
//		if (csrfParam == null || csrfToken == null) {
//			throw new XmlPullParserException("No CSRF Token");
//		}
		if (masterParam == null || masterToken == null) {
			throw new XmlPullParserException("No Master Token");
		}
	}
	
	private String changeType(String url, String newType) throws MalformedURLException {
		URL nurl = new URL(url);
		String path = nurl.getPath();
		String query = nurl.getQuery();
		String proto = nurl.getProtocol();
		int port = nurl.getPort();
		String host = nurl.getHost();
		String auth = nurl.getAuthority();
		
		int typeIdx = path.lastIndexOf(".");
		String type = (typeIdx < 0 ? null : path.substring(typeIdx +1));
		path = (typeIdx < 0 ? path : path.substring(0,typeIdx));
		
		type = newType;
		
		StringBuilder sb = new StringBuilder();
		sb.append(proto == null ? "http" : proto);
		sb.append("://");
		sb.append(host);
		sb.append(port < 0 ? "" : ":" + port);
		sb.append(auth == null ? "" : "@" + auth);
		sb.append(path == null ? "/" : "/" + path);
		sb.append(type == null ? "" : "." + type);
		sb.append(query == null ? "" : "?" + query);
		
		return new URL(sb.toString()).toExternalForm();
	}
	
	public String login(String emailAddress, String accountPassword, String url) throws ClientProtocolException, IOException, SecurityException {
		Log.d(LOGTAG, "CheckingAccountActivity.login(uid="+Binder.getCallingUid()+", name="+emailAddress+", password="+accountPassword+", url="+url+")");

		final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		try {
			// We may be logged in here due to session cookies. But we are really trying to 
			// login without relogging in.
			getJsonTokens(url);
			// This will be invalid since we will clear the session cookie, because it may contain
			// an already logged in user, causing the login to be subverted. We still need the master token
			// as it may change.
			//params.add(new BasicNameValuePair(String.format(PARAM_CSRF_TOKEN_FMT, csrfParam), csrfToken));
			params.add(new BasicNameValuePair(String.format(PARAM_MASTER_FMT, masterParam), masterToken));
		} catch (IOException e1) {
			throw e1;
		}
		params.add(new BasicNameValuePair(PARAM_LOGIN, emailAddress));
		params.add(new BasicNameValuePair(PARAM_PASSWORD, accountPassword));
		params.add(new BasicNameValuePair("muni_admin[remember_me]", "1"));
		HttpEntity entity;
		try {
			entity = new UrlEncodedFormEntity(params);
		} catch (UnsupportedEncodingException e) {
			// this should never happen.
			throw new IllegalStateException(e);
		}
		
		HttpPost request = new HttpPost(changeType(url, "json"));
		// We are going to log in with credentials. We want to clear
		// any other credentials that may have come back.
		// This causes the CRSF token to be invalid, and it will reset
		// the session, which is okay, because we want to log in as 
		// somebody new anyway. 
		cookieStore.clear();
		request.setEntity(entity);
		HttpResponse resp = httpClient.execute(request,localHttpContext);
		Header[] headers = resp.getAllHeaders();
		for (Header h : headers) {
			Log.d(LOGTAG, "Header: " + h.getName() + " " + h.getValue());
		}
		HttpEntity ent = resp.getEntity();
		ent.consumeContent();
		Log.d(LOGTAG, resp.getStatusLine().getStatusCode() + " " + resp.getStatusLine().getReasonPhrase());
		if (400 <= resp.getStatusLine().getStatusCode()) {
			throw new SecurityException("Authentication Failed.");
		} else {
			return retrieveAuthToken(headers);
		}
	}
	
	private String retrieveAuthToken(Header[] headers) {
		String authToken = null;
		for (Header h : headers) {
			// We must go through them all because there could be multiple
			// set-cookie for the same cookie. In fact, some version of Rails, puts a 
			// Set-Cookie auth_token=; header before another with
			// the actual token.
			if (h.getName().equals("Set-Cookie")
					&& h.getValue().startsWith("auth_token=")) {
				// Set-Cookie auth_token=4abec3341; path=/; expires=.....
				String[] splits = h.getValue().split("=");
				splits = splits[1].split(";");
				if (splits[0].equals(authToken)) {
					// This should never happen.
					Log.d(LOGTAG, "Logged in with right token");
				} else {
					Log.d(LOGTAG, "We must replace auth_token " + splits[0]);
					authToken = splits[0];
				}
			}
		}
		return authToken;
	}
}
