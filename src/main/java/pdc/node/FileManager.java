package pdc.node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {

    public void writeFile(String dirName, String fileName, byte[] bytes) {
        String userDirectoryPath = System.getProperty("user.dir");
		File outputFile = new File(userDirectoryPath+"/nodes/"+dirName+"/"+fileName);
        File parentDir = outputFile.getParentFile();

        if (!parentDir.exists()) {
			if (parentDir.mkdirs()) {
				System.out.println("Directories created successfully.");
			} else {
				System.out.println("Failed to create directories.");
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(outputFile);
			fos.write(bytes);
			fos.close();
		} catch (IOException e) { System.out.println("IO error FOS writing new file "); e.printStackTrace(); }
		System.out.println("File stored");
    }

    public byte[] readFile(String fileString) {
        Path filePath = Paths.get(fileString);
        byte[] data = null;
        try { data = Files.readAllBytes(filePath); } catch (IOException e) { System.out.println("IO error reading file bytes to send"); e.printStackTrace(); }
        return data;
    }

	public byte[] getURLFile(String dirName, String fileName, URL url) {
		try (InputStream istream = url.openStream()) {
			return istream.readAllBytes();
		}
		catch(IOException e) { System.out.println("IO error writing url to file"); e.printStackTrace(); }
		return null;
	}
}
