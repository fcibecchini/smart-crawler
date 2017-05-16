package it.uniroma3.crawler.util;

public class FileUtils {
	
	public static String normalizeURL(String url) {
		return url.replaceAll("http[s]?://(www.)?", "").replaceAll("\\.", "_");
	}
	
    public static String getWriteDir(String root, String website) {
		String dir = root +"/"+ normalizeURL(website);
		return dir;
    }
    
    public static String getMirror(String root, String website) {
    	return getWriteDir(root, website)+"_mirror";
    }

}
