package i5.las2peer.services.noracleService.model;

import java.io.Serializable;

public class Question implements Serializable {

	private static final long serialVersionUID = 1L;

	private String questionId;
	private String text;
	private String spaceId;
	private String authorId;
	private String timestampCreated;
	private String timestampLastModified;
	private int depth;

	public Question() { // used in tests
	}

	public Question(String questionId, String text, String spaceId, String authorId, String timestampCreated) {
		this.questionId = questionId;
		this.text = text;
		this.spaceId = spaceId;
		this.authorId = authorId;
		this.timestampCreated = timestampCreated;
		this.timestampLastModified = timestampCreated;
		this.depth = 0;
	}

	public String getQuestionId() {
		return questionId;
	}

	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public String getAuthorId() {
		return authorId;
	}

	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	public String getTimestampCreated() {
		return timestampCreated;
	}

	public void setTimestampCreated(String timestampCreated) {
		this.timestampCreated = timestampCreated;
	}

	public String getTimestampLastModified() {
		return timestampLastModified;
	}

	public void setTimestampLastModified(String timestampLastModified) {
		this.timestampLastModified = timestampLastModified;
	}

	public int getDepth() {
		return this.depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

}
