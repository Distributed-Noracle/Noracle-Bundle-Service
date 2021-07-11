package i5.las2peer.services.noracleService.pojo;

import java.io.Serializable;

public class CreateQuestionPojo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
