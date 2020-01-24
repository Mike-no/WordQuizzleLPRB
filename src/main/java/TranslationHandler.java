/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

// Class that manages the file containing the words to be translated and the HTTP communication with the translation site
public class TranslationHandler {
	private File wordsFile;
	private ArrayList<String> wordsLst;
	
	private static final String WORDS_FILE_PATH 	 = "./txt/italian_words.txt";
	// My github
	private static final String WORDS_FILE_URL		 = "https://github.com/Mike-no/txt_it_word/raw/master/italian_words.txt";
	
	private static final String TRANSLATION_SITE_URL = "https://api.mymemory.translated.net/get?q=";
	private static final String LANGPAIR			 = "&langpair=it|en";

	private static final String JSON_MATCHES		 = "matches";
	private static final String JSON_TRANSLATION	 = "translation";
	
	// Constructor
	public TranslationHandler() {
		wordsFile = new File(WORDS_FILE_PATH);
		
		try {
			if(!wordsFile.exists()) {
				URL website = null;
				wordsFile.createNewFile();
				
				// Download the words file if it doesn't already exist
				website = new URL(WORDS_FILE_URL);

				try(
					ReadableByteChannel rbc = Channels.newChannel(website.openStream());
					FileChannel outChannel = FileChannel.open(wordsFile.toPath(), StandardOpenOption.WRITE);
				){
					outChannel.transferFrom(rbc, 0, Long.MAX_VALUE);
				} catch (IOException ioe) {
					ErrorMacro.ioExceptionHandling(ioe);
				}
			}
			
			// Builds the linked list containing the words in the file
			try(BufferedReader br = new BufferedReader(new FileReader(wordsFile))){
				wordsLst = new ArrayList<String>();
				String line;
				
				while((line = br.readLine()) != null)
					wordsLst.add(line);
			} catch (IOException ioe) {
				ErrorMacro.ioExceptionHandling(ioe);
			}
		} catch (MalformedURLException mue) {
			ErrorMacro.malformedUrlExceptionHandling(mue);
		}catch (IOException ioe) {
			ErrorMacro.ioExceptionHandling(ioe);
		} 
	}
	
	/**
	 * Select 10 random words from word files
	 * @return array list containing ten randomly chosen words
	 */
	private ArrayList<String> tenRandomWords() {
		ArrayList<String> arrLst = new ArrayList<String>();

		for(int i = 0; i < 10; i++)
			arrLst.add(wordsLst.get(ThreadLocalRandom.current().nextInt(0, wordsLst.size())));
		
		return arrLst;
	}
	
	/**
	 * Calls the tenRandomWord () function and returns an array of linked lists <String>;
	 * each LinkedList<String> contains the corresponding Italian term in position 0 and 
	 * the possible translations in the other indexes.
	 * @return array of linked lists
	 */
	public LinkedList<String>[] getWords() throws IOException {
		ArrayList<String> tmpLst = tenRandomWords();
		@SuppressWarnings("unchecked") // Safe cast
		LinkedList<String>[] retLst = (LinkedList<String>[])new LinkedList[tmpLst.size()];
		
		for(int i = 0; i < tmpLst.size(); i++) {
			URL url = new URL(TRANSLATION_SITE_URL + URLEncoder.encode(tmpLst.get(i), MyUtilities.ENCODING) + LANGPAIR);
			
			System.out.println(url.toExternalForm());		// Resulting link
			
			URLConnection urlConn = url.openConnection();
			
			StringBuilder jsonResponse = new StringBuilder();
			String line;
			
			// Reads the json object returned by the get response
			BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			while((line = br.readLine()) != null)
				jsonResponse.append(line);
			
			// Builds the array to return
			retLst[i] = new LinkedList<String>();
			retLst[i].add(tmpLst.get(i));
			
			JsonObject jobj = new Gson().fromJson(jsonResponse.toString(), JsonObject.class);
			JsonArray jsonArr = jobj.get(JSON_MATCHES).getAsJsonArray();
			for(int j = 0; j < jsonArr.size(); j++) {
				JsonObject tmpJobj = jsonArr.get(j).getAsJsonObject();
				retLst[i].add(tmpJobj.get(JSON_TRANSLATION).getAsString().toLowerCase());
			}				
		}
		
		return retLst;
	}
}
