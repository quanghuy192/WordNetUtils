package vn.hn.khmt.qhuy.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NegativeWordNetUtil {

	public static final String BASE_NEGATIVE_WORDNET_LINK = "http://viet.wordnet.vn/wnms/visualize/xml/1699-1699-10333";
	private final String SIMILARITY_LABEL = "tương tự";
	private final String NEGATIVE_WORDNET_FILE = "negative.txt";
	private String negative_word_wordnet_link = "";
	private Document document;
	private List<Word> wordnetForNegative;
	private boolean isFirstTimeGoodWord = true;
	private FileWriter writer;
	private BufferedWriter bufferedWriter;

	public NegativeWordNetUtil() {
		setSourceType();
	}

	public void setSourceType() {

		// Init list
		wordnetForNegative = new ArrayList<>();
		negative_word_wordnet_link = BASE_NEGATIVE_WORDNET_LINK;

		// init for write output wordnet file
		try {
			writer = new FileWriter(new File(NEGATIVE_WORDNET_FILE));
			bufferedWriter = new BufferedWriter(writer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Get default url to
		// load data for good word
		setUrlSource();
	}

	void setUrlSource() {
		try {
			document = Jsoup.connect(negative_word_wordnet_link).maxBodySize(0).timeout(600000).userAgent("Mozilla")
					.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getUnscapeCharLink(String link) {
		String linkRemoveLT = link.replaceAll("&lt;", "<");
		String linkRemoveGT = linkRemoveLT.replaceAll("&gt;", ">");
		String linkRemoveQuot = linkRemoveGT.replaceAll("&quot;", "\"");
		return linkRemoveQuot;
	}

	private List<Word> getSimilarityWordList() {
		String links = document.select("pre[class=brush: xml]").text();
		String unscapeCharLink = getUnscapeCharLink(links);

		document = Jsoup.parse(unscapeCharLink);

		int count = 1;
		List<Word> similarityWordList = new ArrayList<>();

		// Get data only to level 1 and level 2
		while (count <= 2) {
			String conditionId = new StringBuilder("node[id=sense-").append(count).append("]").toString();

			// First time we use sense-2 only to get "good word"
			if (isFirstTimeGoodWord && count == 1) {
				isFirstTimeGoodWord = false;
				conditionId = "node[id=sense-2]";
			}

			Elements elementsSense = document.select(conditionId);
			Word word = null;

			for (Element e : elementsSense) {
				Elements subNode = e.select("node[id=N1001B]");
				for (Element e1 : subNode) {
					Elements childTag = e1.children();
					String typeStr = e1.child(0).text();
					if (SIMILARITY_LABEL.equalsIgnoreCase(typeStr)) {
						Elements chElements1 = childTag.select("node[id=N1001FW28129]");
						for (Element e2 : chElements1) {
							String labelW = e2.select("label").text();
							String linkW = e2.select("a").attr("href");
							word = new Word(typeStr, labelW, linkW, 0, false);

							// Check duplicate word
							if (!isExist(wordnetForNegative, word)) {
								similarityWordList.add(word);
							}
						}
					}
				}
			}
			count++;
		}
		for (Word word : similarityWordList) {
			System.out.println(word.getWord() + "-");
		}
		return similarityWordList;
	};

	private boolean isExist(List<Word> wordList, Word word) {
		for (Word w : wordList) {
			if (w.equals(word)) {
				return true;
			}
		}
		return false;
	}

	private Word getSimilarityNeiborWord() {

		for (Word word : wordnetForNegative) {
			if (!word.isCheck()) {
				word.setCheck(true);
				return word;
			}
		}
		return null;
	}

	public List<Word> getSimilarityWord() {

		// Get default word
		List<Word> similarityWordList = getSimilarityWordList();
		wordnetForNegative.addAll(similarityWordList);
		writeFile(similarityWordList, true);

		Word neirborWord = getSimilarityNeiborWord();
		while (null != neirborWord) {
			if (SIMILARITY_LABEL.equalsIgnoreCase(neirborWord.getType())) {
				negative_word_wordnet_link = neirborWord.getLink();
				setUrlSource();
				List<Word> similarityWordListNeibor = getSimilarityWordList();
				wordnetForNegative.addAll(similarityWordListNeibor);
				writeFile(similarityWordListNeibor, true);
				neirborWord = getSimilarityNeiborWord();
			}
		}
		return wordnetForNegative;
	}

	private void writeFile(List<Word> wordsList, boolean isPositiveFile) {

		BufferedWriter write = null;

		write = bufferedWriter;

		try {
			for (Word w : wordsList) {
				write.write(w.getWord());
				write.write("\n");
				write.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			closeWrite();
		}
	}

	private void closeWrite() {
		try {
			bufferedWriter.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}