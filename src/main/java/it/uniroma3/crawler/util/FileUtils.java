package it.uniroma3.crawler.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

public class FileUtils {
	
	/**
	 * Returns a normalized version of this URL, without protocol and dots.
	 * @param url
	 * @return the normalized URL
	 */
	public static String normalizeURL(String url) {
		return url.replaceAll("http[s]?://(www.)?", "")
				.replaceAll("\\.|/|\\?|\\(|\\)", "_");
	}
	
    /**
     * Returns the path to the record directory assigned to this website.<br>
     * The Record directory contains any record extracted from 
     * the crawled html pages.
     * @param website the website
     * @return the directory path
     */
    public static String getRecordDirectory(String website) {
		return "html/"+ normalizeURL(website);
    }
    
    /**
     * Returns the path to a temporary directory assigned to this website.
     * The temporary directory can be used to store crawled pages 
     * for a one-time access.
     * @param website
     * @return the directory path
     */
    public static String getTempDirectory(String website) {
    	return "temp/"+normalizeURL(website);
    }
    
    /**
     * Returns the path to the pages directory assigned to this website.<br>
     * The Pages directory contains the crawled html pages.
     * @param root the root directory
     * @param website the website
     * @return the directory path
     */
    public static String getPagesDirectory(String website) {
    	return "html/"+normalizeURL(website)+"_mirror";
    }
    
    /**
     * Deletes the temporary directory assigned to this website.
     * @param website
     */
    public static void clearTempDirectory(String website) {
    	try {
			Files.walk(Paths.get(getTempDirectory(website)), FOLLOW_LINKS)
		    .sorted(Comparator.reverseOrder())
		    .map(Path::toFile)
		    .forEach(File::delete);
		} catch (Exception e) {}
    }

}
