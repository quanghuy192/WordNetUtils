package vn.hn.khmt.qhuy.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WordNetUtils {

	public static final String BASE_GOOD_WORDNET_LINK = "http://viet.wordnet.vn/wnms/visualize/xml/1699-1699-14444";
	public static final String BASE_BAD_WORDNET_LINK = "http://viet.wordnet.vn/wnms/visualize/xml/1699-1699-3578";
	public static final String BASE_FAST_WORDNET_LINK = "http://viet.wordnet.vn/wnms/visualize/xml/1699-1699-24807";
	public static final String BASE_SLOW_WORDNET_LINK = "http://viet.wordnet.vn/wnms/visualize/xml/1699-1699-5936";
	public static final String BASE_NICE_WORDNET_LINK = "http://viet.wordnet.vn/wnms/visualize/xml/1699-1699-5549";
	public static final String BASE_UGLY_WORDNET_LINK = "http://viet.wordnet.vn/wnms/visualize/xml/1699-1699-3578";

	private final String BLANK = "";
	private final String SIMILARITY_LABEL = "tương tự";
	private final String ANTONYMY_LABEL = "từ trái nghĩa";

	private final String POSITIVE_WORDNET_FILE = "positive_wordnet.txt";
	private final String NEGATIVE_WORDNET_FILE = "negative_wordnet.txt";

	private String good_word_wordnet_link = "";
	private String bad_word_wordnet_link = "";

	private Document documentForGood;
	private Document documentForBad;

	private List<Word> wordnetForPositive;
	private List<Word> wordnetForNegative;

	private boolean isFirstTimeGoodWord = true;
	private boolean isFirstTimeBadWord = true;

	private FileWriter writerPositive, writerNegative;
	private BufferedWriter bufferedWriterPositive, bufferedWriterNegative;

	private Type type;

	public WordNetUtils() {
		this(BASE_GOOD_WORDNET_LINK, BASE_BAD_WORDNET_LINK);
	}

	private WordNetUtils(String positiveLink, String negativeLink) {

		// Init list
		wordnetForPositive = new ArrayList<>();
		wordnetForNegative = new ArrayList<>();

		// Default link
		good_word_wordnet_link = positiveLink;
		bad_word_wordnet_link = negativeLink;

		// init for write output wordnet file
		try {
			writerPositive = new FileWriter(new File(POSITIVE_WORDNET_FILE));
			bufferedWriterPositive = new BufferedWriter(writerPositive);

			writerNegative = new FileWriter(new File(NEGATIVE_WORDNET_FILE));
			bufferedWriterNegative = new BufferedWriter(writerNegative);
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
			documentForGood = Jsoup.connect(good_word_wordnet_link).maxBodySize(0).timeout(600000).userAgent("Mozilla")
					.get();
			documentForBad = Jsoup.connect(bad_word_wordnet_link).maxBodySize(0).timeout(600000).userAgent("Mozilla")
					.get();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getWordNetBodyLink(Type type) {
		if (type == Type.GOOD || type == Type.FAST || type == Type.NICE) {
			return documentForGood.select("pre[class=brush: xml]").text();
		}
		if (type == Type.BAD || type == Type.SLOW || type == Type.UGLY) {
			return documentForBad.select("pre[class=brush: xml]").text();
		}
		return BLANK;
	}

	private String getUnscapeCharLink(String link) {
		String linkRemoveLT = link.replaceAll("&lt;", "<");
		String linkRemoveGT = linkRemoveLT.replaceAll("&gt;", ">");
		String linkRemoveQuot = linkRemoveGT.replaceAll("&quot;", "\"");
		return linkRemoveQuot;
	}

	private List<Word> getSimilarityWordList(Type type) {
		String links = getWordNetBodyLink(type);
		String unscapeCharLink = getUnscapeCharLink(links);

		documentForGood = Jsoup.parse(unscapeCharLink);

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

			Elements elementsSense = documentForGood.select(conditionId);

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
							if (!isExist(wordnetForPositive, word)) {
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

	private List<Word> getAntonymyWordList(Type type) {
		String links = getWordNetBodyLink(type);
		String unscapeCharLink = getUnscapeCharLink(links);

		documentForBad = Jsoup.parse(unscapeCharLink);

		int count = 1;
		List<Word> antonymyList = new ArrayList<>();

		// Get data only to level 1 and level 2
		while (count <= 2) {
			String conditionId = new StringBuilder("node[id=sense-").append(count).append("]").toString();

			Elements elementsSense = documentForBad.select(conditionId);

			Word word = null;

			for (Element e : elementsSense) {
				Elements subNode = e.select("node[id=N1001B]");
				for (Element e1 : subNode) {

					Elements childTag = e1.children();
					String typeStr = e1.child(0).text();
					if (SIMILARITY_LABEL.equalsIgnoreCase(typeStr)) {

						// First time we use sense-2 only to get "bad word"
						if (isFirstTimeBadWord) {
							isFirstTimeBadWord = false;
							continue;
						}

						Elements chElements1 = childTag.select("node[id=N1001FW28129]");

						for (Element e2 : chElements1) {
							String labelW = e2.select("label").text();
							String linkW = e2.select("a").attr("href");
							word = new Word(typeStr, labelW, linkW, 0, false);

							// Check duplicate word
							if (!isExist(wordnetForNegative, word)) {
								antonymyList.add(word);
							}
						}
					}
				}
			}
			count++;
		}
		for (Word word : antonymyList) {
			System.out.println(word.getWord() + "-");
		}
		return antonymyList;
	}

	private boolean isExist(List<Word> wordList, Word word) {
		for (Word w : wordList) {
			if (w.equals(word)) {
				return true;
			}
		}
		return false;
	}

	private Word getSimilarityNeiborWord() {

		for (Word word : wordnetForPositive) {
			if (!word.isCheck()) {
				word.setCheck(true);
				return word;
			}
		}
		return null;
	}

	private Word getAntonymyNeiborWord() {

		for (Word word : wordnetForNegative) {
			if (!word.isCheck()) {
				word.setCheck(true);
				return word;
			}
		}
		return null;
	}

	private List<Word> getSimilarityWord(Type type) {

		// Get default word
		List<Word> similarityWordList = getSimilarityWordList(type);
		wordnetForPositive.addAll(similarityWordList);
		writeFile(similarityWordList, true);

		Word neirborWord = getSimilarityNeiborWord();
		while (null != neirborWord) {
			if (SIMILARITY_LABEL.equalsIgnoreCase(neirborWord.getType())) {
				good_word_wordnet_link = neirborWord.getLink();
				setUrlSource();
				List<Word> similarityWordListNeibor = getSimilarityWordList(type);
				wordnetForPositive.addAll(similarityWordListNeibor);
				writeFile(similarityWordListNeibor, true);
				neirborWord = getSimilarityNeiborWord();
			}
		}
		return wordnetForPositive;
	}

	private List<Word> getAntonymyWord(Type type) {

		// Get default word
		List<Word> antonymyWordList = getAntonymyWordList(type);
		wordnetForNegative.addAll(antonymyWordList);
		writeFile(antonymyWordList, false);

		Word neirborWord = getAntonymyNeiborWord();
		while (null != neirborWord) {
			if (SIMILARITY_LABEL.equalsIgnoreCase(neirborWord.getType())) {
				bad_word_wordnet_link = neirborWord.getLink();
				setUrlSource();
				List<Word> antonymyWordListNeibor = getAntonymyWordList(type);
				wordnetForNegative.addAll(antonymyWordListNeibor);
				writeFile(antonymyWordListNeibor, false);
				neirborWord = getAntonymyNeiborWord();
			}
		}
		return wordnetForNegative;
	}

	private void writeFile(List<Word> wordsList, boolean isPositiveFile) {

		BufferedWriter write = null;

		if (isPositiveFile) {
			write = bufferedWriterPositive;
		} else {
			write = bufferedWriterNegative;
		}

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
			bufferedWriterPositive.close();
			writerPositive.close();

			bufferedWriterNegative.close();
			writerNegative.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	static int count = 0;

	private class Task implements Runnable {

		@Override
		public void run() {
			if (type == Type.GOOD || type == Type.FAST || type == Type.NICE) {
				getSimilarityWord(type);
			}
			if (type == Type.BAD || type == Type.SLOW || type == Type.UGLY) {
				getAntonymyWord(type);
			}
		}
	}

	public static void main(String[] args) {
		// WordNetUtils utils = new WordNetUtils();

		Type[] types = { Type.GOOD, Type.BAD, Type.FAST, Type.SLOW, Type.NICE, Type.UGLY };

		for (Type t : types) {
			ExecutorService service = Executors.newFixedThreadPool(1);
			try {
				service.submit(new Callable<String>() {

					@Override
					public String call() throws Exception {
						WordNetUtils utils = new WordNetUtils();

						if (t == Type.GOOD || t == Type.FAST || t == Type.NICE) {
							System.out.println(count++);
							utils.getSimilarityWord(t);
						}
						if (t == Type.BAD || t == Type.SLOW || t == Type.UGLY) {
							System.out.println(count++);
							utils.getAntonymyWord(t);
						}
						return "OK";
					}
				}).get(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				System.out.println("Stop 1");
				service.shutdownNow();
			} catch (ExecutionException e) {
				System.out.println("Stop 2");
				service.shutdownNow();
			} catch (TimeoutException e) {
				System.out.println("Stop 3");
				service.shutdownNow();
			}

		}

		// List<Word> p = utils.getSimilarityWord(Type.GOOD);
		// List<Word> n = utils.getAntonymyWord(Type.BAD);
	}

}
