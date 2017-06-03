package it.uniroma3.crawler.modeler.model;

/**
 * An XPathAttribute represent a attribute of a {@link XPathTag} 
 * with name and value.
 *
 */
public class XPathAttribute {
	private String attribute;
	private String value;
	private short flag;
	
	/**
	 * Constructs a new XPathAttribute with the given name and value.
	 * By default, this attribute is omitted from the tag unless specified.
	 * @param attribute the name of the attribute
	 * @param value the value of the attribute
	 */
	public XPathAttribute(String attribute, String value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	/**
	 * Increment the grain of this attribute.
	 */
	public void increment() {
		flag++;
	}
	
	/**
	 * Decrement the grain of this attribute.
	 */
	public void decrement() {
		flag--;
	}
	
	/**
	 * Omit this attribute from the tag.
	 */
	public void setNotUsed() {
		flag = 0;
	}
	
	/**
	 * Use only the attribute name.
	 */
	public void setAttribute() {
		flag = 1;
	}
	
	/**
	 * Use both the attribute name and value.
	 */
	public void setFinest() {
		flag = 2;
	}
	
	/**
	 * True if both the name and value of this attribute
	 * are used.
	 * @return true if the attribute is in the most specific form
	 */
	public boolean isFinest() {
		return flag>=2;
	}
	
	/**
	 * 
	 * @return true if this attribute is omitted
	 */
	public boolean notUsed() {
		return flag==0;
	}
	
	/**
	 * 
	 * @return true if the grain of this if this attribute can be incremented
	 */
	public boolean canIncrement() {
		return flag<2;
	}
	
	/**
	 * 
	 * @return true if the grain of this attribute can be decremented
	 */
	public boolean canDecrement() {
		return flag>0;
	}
	
	/**
	 * Returns true if the name of this attribute is equal to
	 * the specified attribute
	 * @param attribute
	 * @return true if attribute is equal to the attribute name
	 */
	public boolean is(String attribute) {
		return attribute.equals(attribute);
	}
	
	/**
	 * 
	 * @return the current version of this attribute
	 */
	public String get() {
		StringBuilder attr = new StringBuilder();
		if (flag>0) {
			attr.append("@"+attribute);
			if (flag>1) attr.append("=\""+value+"\"");
		}
		return attr.toString();
	}

}
