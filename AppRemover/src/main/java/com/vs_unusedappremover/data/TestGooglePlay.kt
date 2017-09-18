package com.vs_unusedappremover.data


class TestGooglePlay//	public void getInfo(String packageName) {
//		String numDownloadsString = "<dd itemprop=\"numDownloads\">";
//		int numDownloadsMatches = 0;
//
//		String ratingString = "itemprop=\"ratingValue\" content=\"";
//		int numRatingMatches = 0;
//
//		HttpClient httpClient = new DefaultHttpClient();
//		HttpPost httppost = new HttpPost("http://play.google.com/store/apps/details?id=" + packageName);
//
//		URL url = new URL("http://play.google.com/store/apps/details?id=" + packageName);
//		URLConnection con = url.openConnection();
//
//		String encoding = con.getContentEncoding();
//		encoding = encoding == null ? "UTF-8" : encoding;
//
//		InputStream in = con.getInputStream();
//		try {
//			Reader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(encoding)));
//			int character;
//			while ((character = reader.read()) != -1) {
//				if (character == numDownloadsString.charAt(numDownloadsMatches)) {
//					numDownloadsMatches++;
//				} else {
//					numDownloadsMatches = 0;
//				}
//
//				if (character == ratingString.charAt(numRatingMatches)) {
//					numRatingMatches++;
//				} else {
//					numRatingMatches = 0;
//				}
//
//
//			}
//
//		} finally {
//			in.close();
//		}
//
//
//
//		String body = IOUtils.toString(in, encoding);
//		System.out.println(body);
//
//
//		try {
//		    // Execute HTTP Post Request
//		    HttpResponse response = httpClient.execute(httppost);
//		    response.
//
//		} catch (ClientProtocolException e) {
//		    // ...
//		} catch (IOException e) {
//		    // ...
//		}
//	}
//
//	public boolean streamContainsString(Reader reader, String searchString) throws IOException {
//	    char[] buffer = new char[1024];
//	    int numCharsRead;
//	    int count = 0;
//	    while((numCharsRead = reader.read(buffer)) > 0) {
//	        for (int c = 0; c < numCharsRead; c++) {
//	            if (buffer[c] == searchString.charAt(count))
//	                count++;
//	            else
//	                count = 0;
//	            if (count == searchString.length()) return true;
//	        }
//	    }
//	    return false;
//	}
