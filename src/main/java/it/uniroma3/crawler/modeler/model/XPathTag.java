package it.uniroma3.crawler.modeler.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An XPathTag represent a tag of a DOM tree belonging to a {@link XPath}
 * instance. 
 * 
 */
public class XPathTag implements Comparable<XPathTag> {
	private int index;
	private String name;
	private List<XPathAttribute> attributes;
	private boolean use, hasId;
	
	/**
	 * Constructs a new XPathTag with the given name and index.<br>
	 * @param name the name of this tag
	 * @param index index of this tag in traversal order from the last node to the
	 * root
	 */
	public XPathTag(String name, int index) {
		this.name = name;
		this.index = index;
		this.attributes = new ArrayList<>();
		this.use = true;
	}
	
	public XPathTag(XPathTag xpathTag) {
		this.name = xpathTag.getName();
		this.index = xpathTag.getIndex();
		this.attributes = new ArrayList<>();
		xpathTag.getAttributes().forEach(a -> attributes.add(new XPathAttribute(a)));
		this.use = xpathTag.used();
		this.hasId = xpathTag.hasId();
	}
	
	/**
	 * 
	 * @return the position of this tag in the path from a node to the root
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * 
	 * @return the name of the tag
	 */
	public String getName() {
		return name;
	}
	
	public List<XPathAttribute> getAttributes() {
		return attributes;
	}
	
	/**
	 * Set if this tag should be used in the XPath.
	 * @param use boolean
	 */
	public void setUse(boolean use) {
		this.use = use;
	}
	
	/**
	 * 
	 * @return true if this tag is currently used in the XPath
	 */
	public boolean used() {
		return use;
	}
	
	/**
	 * Add a new attribute to this XPath tag.
	 * @param attribute
	 * @param value
	 */
	public void addAttribute(String attribute, String value) {
		attributes.add(new XPathAttribute(attribute, value));
		if (!hasId && attribute.equals("id")) hasId = true;
	}
	
	/**
	 * Increments the granularity of this tag by enabling the tag
	 * or refining its attributes and values.
	 */
	public void increment() {
		if (!use) use = true;
		else {
			attributes.stream()
			.filter(XPathAttribute::canIncrement)
			.findFirst().ifPresent(XPathAttribute::increment);
		}
	}
	
	/**
	 * Decrements the granularity of this tag by disabling the tag
	 * or one of its attributes and values.
	 */
	public void decrement() {
		XPathAttribute at = attributes.stream()
		.filter(XPathAttribute::canDecrement)
		.reduce((__,a) -> a).orElse(null);
		if (at==null) use = false;
		else at.decrement();
	}
	
	/**
	 * 
	 * @return true if this tag is at its finest version
	 */
	public boolean isFinest() {
		return !canIncrement();
	}
	
	/**
	 * 
	 * @return true if the grain of this tag can be incremented
	 */
	public boolean canIncrement() {
		return !use || (!attributes.isEmpty() &&
				attributes.stream().anyMatch(XPathAttribute::canIncrement));
	}
	
	/**
	 * 
	 * @return true if the grain of this tag can be decremented
	 */
	public boolean canDecrement() {
		return (use && index!=0) || (!attributes.isEmpty() &&
				attributes.stream().anyMatch(XPathAttribute::canDecrement));
	}
	
	/**
	 * 
	 * @return true if this tag has no attributes
	 */
	public boolean isEmpty() {
		return attributes.isEmpty();
	}
	
	/**
	 * 
	 * @return true if this tag contains a "id" attribute
	 */
	public boolean hasId() {
		return hasId;
	}
	
	/**
	 * Set this tag to use only the id attribute and its value.<br>
	 * e.g. : <code>div -> div[@id="value"]</code>
	 */
	public void setIdAndValue() {
		attributes.stream().filter(a -> a.is("id"))
		.findFirst().ifPresent(XPathAttribute::setFinest);
	}
	
	/**
	 * Set this tag to use only the id attribute.<br>
	 * e.g. : <code>div -> div[@id]</code>
	 */
	public void setId() {
		attributes.stream().filter(a -> a.is("id"))
		.findFirst().ifPresent(XPathAttribute::setAttribute);
	}
	
	/**
	 * Set this tag to use all of its attributes without the values<br>
	 * e.g. : <code>a -> a[@class and @title]</code>
	 */
	public void setAllAttributes() {
		attributes.stream().filter(XPathAttribute::canIncrement)
		.forEach(XPathAttribute::setAttribute);
	}
	
	/**
	 * Set this tag to use only its first attribute without the value<br>
	 * e.g. : <code>div -> div[@class]</code>
	 */
	public void setFirstAttribute() {
		attributes.stream().filter(XPathAttribute::canIncrement)
		.findFirst().ifPresent(XPathAttribute::setAttribute);
	}
	
	/**
	 * Set this tag to use all of its attributes and their value<br>
	 * e.g. : <code>a -> a[@class="name" and @id="value"]</code>
	 */
	public void setFinest() {
		use = true;
		attributes.forEach(XPathAttribute::setFinest);
	}
	
	/**
	 * Omit this tag from the XPath path. If this tag is the anchor, 
	 * set only the tag name.
	 */
	public void omit() {
		if (index!=0) use = false;
		attributes.forEach(XPathAttribute::setNotUsed);
	}
	
	private String values() {
		StringBuilder values = new StringBuilder();
		attributes.stream()
		.filter(XPathAttribute::canDecrement)
		.forEach(a -> values.append(a.get()+" and "));
		int len = values.length();
		return (len>0) ? values.substring(0, len-5).toString() : "";
	}
	
	/**
	 *  
	 * @return the current version of this tag
	 */
	public String get() {
		if (!use) return "";
		StringBuilder tag = new StringBuilder(name);
		if (!attributes.isEmpty()) {
			String values = values();
			if (!values.isEmpty())
				tag.append("["+values+"]");
		}
		return tag.toString();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(index,name);
	}
	
	@Override
	public boolean equals(Object obj) {
		XPathTag o = (XPathTag) obj;
		return Objects.equals(index, o.getIndex()) &&
				Objects.equals(name, o.getName());
	}

	@Override
	public int compareTo(XPathTag o) {
		int cmpIndex = o.getIndex() - index;
		return (cmpIndex!=0) ? cmpIndex : name.compareTo(o.getName());
	}
}
