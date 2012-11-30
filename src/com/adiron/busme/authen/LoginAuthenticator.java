package com.adiron.busme.authen;

import java.io.IOException;

interface LoginAuthenticator {
	String login(String name, String password, String url) throws IOException, SecurityException;
}
