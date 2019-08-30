package com.google.appinventor.server;

import java.io.IOException;

import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;

import com.google.appengine.api.users.UserServiceFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

/**
 * Handles redirect from Google Auth after either giving or denying permission to access 
 * Google Drive and redirects users to the home page. Creates and saves access and refresh 
 * tokens to access users' Google Drive and - for the time being - saves nothing for 
 * users that deny permission.
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 *
 */
@SuppressWarnings("unchecked")
public class CredentialServlet extends HttpServlet {
	
	private final StorageIo storageIo = StorageIoInstanceHolder.INSTANCE;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		System.out.println("Credential Servlet Get Reached");
		resp.setContentType("text/html; charset=utf-8");
		
		PrintWriter out;
		
		String code = req.getParameter("code");
		String uri = req.getParameter("uri");
		String userId = UserServiceFactory.getUserService().getCurrentUser().getUserId();
		
//		if (code == null) {
//			System.out.println(req.getRequestURI());
//			System.out.println("User did not grant permission to access Drive");
//			out = resp.getWriter();
//			out.println("<html><body>\n");
//			out.println("<div style=\"text-align: center;\">");
//			out.println("<h1>Prevent future requests to access Drive?</h1>\n\n");
//			out.println("<form method=POST action=\"" + uri + "\">");
//			out.println("<input type=Submit name=\"yesOrNo\" value=\"yes\" style=\"font-size: 200%;\">\n");
//			out.println("<input type=Submit name=\"yesOrNo\" value=\"no\" style=\"font-size: 200%;\">\n");
//			out.println("</div></form></body></html>");
//			return;
//		} 
		
		if (code != null) {
			try {
				final String redirect = req.getRequestURL() + "?uri=" + uri;
				GoogleUtils.createAndSaveCredential(userId, code, redirect);
				System.out.println("Credential saved for: " + userId + ", code: " + code);
				// Enable Google Drive backup for the current user
				storageIo.enableUserDrive(userId, true);
			} catch (Exception e) {
				e.printStackTrace(); 
			}
		} else {
			storageIo.enableUserDrive(userId, false);
		}
		
		resp.sendRedirect(uri);
		return;
	}
	
//	@Override
//	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//		System.out.println("Credential Servlet Post reached");
//		resp.setContentType("text/html; charset=utf-8");
//		
//		String access = req.getParameter("yesOrNo");
//		String userId = userInfoProvider.getUserId();
//		String redirectUri = req.getParameter("uri");
//		
//		// Not sure if this can happen
//		assert(access != null);
//		
//		if (access.equals("yes")) {
//			storageIo.enableUserDrivePermissionRequests(userId, false);
//			userInfoProvider.enableDrivePermissionRequests(false);
//		} else {
//			storageIo.enableUserDrivePermissionRequests(userId, true);
//			userInfoProvider.enableDrivePermissionRequests(true);
//		}
//		storageIo.enableUserDrive(userId, false);
//		userInfoProvider.enableDrive(false);
//		
//		resp.sendRedirect(redirectUri);
//		return;
//	}
}