package it.uniroma3.crawler.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class FileUtils {
	
    public static String getWriteDir(String root, String website) {
		String dir = root +"/"+ website.replaceAll("http[s]?://(www.)?", "").replaceAll("\\.", "_");
		return dir;
    }
    
    public static String getMirror(String root, String website) {
    	return getWriteDir(root, website)+"_mirror";
    }
    
    public static Properties getProperties(String fileName) {
    	try (InputStream stream = Files.newInputStream(Paths.get(fileName))) {
        	Properties config = new Properties();
            config.load(stream);
            return config;
        } catch (IOException ie) {
        	return null;
        }
    }

}
