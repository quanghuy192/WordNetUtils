package vn.hn.khmt.qhuy.utils;

public class Word {

    private final int HASHCODE_CONST = 17;

    private String    type;
    private String    word;
    private String    link;
    private int	      distance;
    private boolean   isCheck;

    public Word() {
    }

    public Word(String type, String word, String link, int distance,
            boolean isCheck) {
	super();
	this.type = type;
	this.word = word;
	this.link = convertToXmlLink(link);
	this.distance = distance;
	this.isCheck = isCheck;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public String getLink() {
	return link;
    }

    public void setLink(String link) {
	this.link = convertToXmlLink(link);
    }

    public String getWord() {
	return word;
    }

    public void setWord(String word) {
	this.word = word;
    }

    public int getDistance() {
	return distance;
    }

    public void setDistance(int distance) {
	this.distance = distance;
    }

    public boolean isCheck() {
	return isCheck;
    }

    public void setCheck(boolean isCheck) {
	this.isCheck = isCheck;
    }

    @Override
    public int hashCode() {

	int typeCod = this.type == null ? 0 : 1;
	int wordCod = this.word == null ? 0 : 1;
	int linkCod = this.link == null ? 0 : 1;
	int priceCod = isCheck ? 0 : 1;

	int result = HASHCODE_CONST;
	result = 31 * result + typeCod;
	result = 31 * result + wordCod;
	result = 31 * result + linkCod;
	result = 31 * result + priceCod;
	result = 31 * result + distance;

	return result;
    }

    @Override
    public boolean equals(Object o) {
	if (o == this)
	    return true;
	if (!(o instanceof Word))
	    return false;
	Word w = (Word) o;
	return w.word.equalsIgnoreCase(this.word);
    }

    @Override
    public String toString() {
	return "" + type + "-" + "-" + word + "-" + link + "-" + distance + "-"
	        + isCheck;
    }

    private String convertToXmlLink(String link) {
	return link.replace("treebolic", "xml");
    }

}
