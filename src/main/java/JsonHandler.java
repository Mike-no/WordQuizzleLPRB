/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.Map;
import java.io.IOException;

// Class dedicated to managing json files, converting to json and reading from json
public class JsonHandler {
	private Gson gson;
	
	private static final String ENCODING  = "UTF-8";
	
	public JsonHandler() {
		gson = new GsonBuilder().setPrettyPrinting().create();
	}
	
	/**
	 * Converts the passed map <response, value> to a json string.
	 * @param map
	 * @return json string
	 * @throws NullPointerException : if map is null
	 */
	public synchronized String toJson(Map<String, String> map) {
		if(map == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		
		return gson.toJson(map);
	}
	
	/**
	 * Converts the passed list <Player> to a json string.
	 * @param lst
	 * @return json string
	 * @throws NullPointerException : if lst is null
	 * @deprecated
	 */
	public synchronized String toJson(LinkedList<Player> lst) {
		if(lst == null)
			throw new NullPointerException();
		
		return gson.toJson(lst);
	}
	
	/**
	 * Converts the passed json String to a map <request, value>.
	 * @param json
	 * @return map<String, String>
	 * @throws NullPointerException : if json is null
	 */
	public Map<String, String> fromJson(String json) {
		if(json == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		
		@SuppressWarnings("unchecked") // Safe cast
		Map<String, String> rqst = (Map<String, String>)gson.fromJson(json, Map.class);
		
		return rqst;
	}
	
	/**
	 * Method for managing the persistence of registration information, 
	 * friendship relationships and scoring users on json files. The method is
	 * synchronized in order to prevent to prevent different threads from writing 
	 * inconsistent information.
	 * @param graph
	 * @param path
	 * @throws IOException
	 * @throws NullPointerException : if graph or path are null
	 */
	public synchronized void writeJSON(PlayerGraph graph, Path path) {
		if(graph == null || path == null)
			throw new NullPointerException(ErrorMacro.NULL_ARGS);
		
		String jsonPorting = gson.toJson(graph);
		byte[] tmpBuf = jsonPorting.getBytes(Charset.forName(ENCODING));
		ByteBuffer buf = ByteBuffer.wrap(tmpBuf);
		buf.put(tmpBuf);
		buf.flip();
		
		try(FileChannel outChannel = FileChannel.open(path, StandardOpenOption.WRITE)){
			while(buf.hasRemaining())
				outChannel.write(buf);
		} catch (IOException ioe) {
			ErrorMacro.ioExceptionHandling(ioe);
		}
	}
	
	/**
	 * method used to upload persistent information from one boot to another on the server.
	 * @param path
	 * @return the graph extrapolated from the json file
	 * @throws IOException
	 * @throws NullPointerException : if path is null
	 */
	public synchronized PlayerGraph readJSON(Path path) {
		if(path == null)
			throw new NullPointerException(ErrorMacro.NULL_ARG);
		
		ByteBuffer buf = ByteBuffer.allocateDirect((int)path.toFile().length());
		
		try(FileChannel inChannel = FileChannel.open(path, StandardOpenOption.READ)){
			inChannel.read(buf);
		} catch (IOException ioe) {
			ErrorMacro.ioExceptionHandling(ioe);
		}
		
		buf.rewind();
		byte[] tmpBuf = new byte[buf.remaining()];
		buf.get(tmpBuf);
		String jsonPorting = new String(tmpBuf, Charset.forName(ENCODING));
		
		PlayerGraph graph = gson.fromJson(jsonPorting, PlayerGraph.class);
		
		return graph;
	}
}
