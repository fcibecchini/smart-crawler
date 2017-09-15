package it.uniroma3.crawler.modeler.model;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.gargoylesoftware.htmlunit.html.DomNode;

/**
 * The XPath class represent a Root-to-link path that matches anchors 
 * in a webpage. Every XPath stores a list of {@link XPathTag} 
 * with {@link XPathAttribute}s and a version of that path currently in use.<br>
 * Different versions of the same path can be defined in terms of attributes and
 * tags used in the generated XPath-to-link: i.e., an XPath can be 
 * refined at runtime by simply adding/removing attributes and tags in the path.  
 *
 */
public class XPath {
	private final String defaultPath;
	private String path;
	private SortedSet<XPathTag> tags;
	
	/**
	 * Creates a new XPath matching the given anchor.<br>
	 * The default, initial version of the XPath starts from a unique element
	 * in the current DOM, such as a node with an "id" attribute or the 
	 * HTML root node.<br> For the anchor node, the default includes 
	 * all its attributes names, or just the id attribute and value if present.
	 * For parent nodes, it includes the first attribute name, or 
	 * just the id attribute and value if present. <br>
	 * For instance, the following Anchor and DOM:
	 * <pre>
	 * {@code <a href="/detail1.html">}<br>
	 * {@code
	 * <html>
	 * <body>
	 *  <div id="main">
	 *    <div id="site_content">
	 *      <div id="content">
	 *        <ul class="list">
	 *         <li><a class="det" href="/detail1.html">Detail page 1</a></li>
	 *         <li><a class="det" href="/detail2.html">Detail page 2</a></li>
	 *         <li><a class="det" href="/detail3.html">Detail page 3</a></li>
	 *        </ul>
	 *      </div>
	 *    </div>
	 *  </div>
	 * </body>
	 * </html>
	 * }
	 * </pre>
	 * will produce the initial version: 
	 * {@code //div[@id='content']/ul[@class]/li/a[@class]}
	 * @param anchor the anchor node to create a Root-to-link path
	 */
	public XPath(DomNode anchor) {
		build(anchor);
		path = "";
		defaultPath = get();
	}
	
	/**
	 * Creates a new XPath that is a copy of the given one.
	 * @param xpath the XPath to copy
	 */
	public XPath(XPath xpath) {
		path = xpath.get();
		defaultPath = xpath.getDefault();
		tags = new TreeSet<>();
		xpath.getTags().forEach(t -> tags.add(new XPathTag(t)));
	}
	
	public SortedSet<XPathTag> getTags() {
		return tags;
	}
	
	/**
	 * Returns the current version of this XPath.
	 * @return the current XPath
	 */
	public String get() {
		if (path.isEmpty()) {
			StringBuilder build = new StringBuilder("/");
			tags.forEach(t -> build.append(t.get()+"/"));
			path = build.toString().replaceFirst("///+", "//")
					.replaceFirst("/$", "");
		}
		return new String(path);
	}
	
	/**
	 * Returns the default version of this XPath, as specified 
	 * by the constructor {@link XPath#XPath(DomNode)}.
	 * @return the default version of this XPath
	 */
	public String getDefault() {
		return defaultPath;
	}
	
	/**
	 * Refines this XPath by adding a tag or attribute starting
	 * from the last tag, so that the
	 * resulting new version is expected to match a smaller collection of links.
	 * A single invocation of this method will result in only one refinement of a
	 * tag or attribute. If a refinement cannot be applied (i.e. this is 
	 * the finest XPath) an empty string is returned. 
	 * <br>Possible refinements are:<br>
	 * <ul>
	 * <li>Add a tag: <code>//font/a -> //div/font/a</code></li>
	 * <li>Add an attribute: <code>//div/font/a/ -> //div[@id]/font/a</code></li>
	 * <li>Add an attribute value: <code>//div[@id]/font/a -> //div[@id="pager_string"]/font/a</code></li>
	 * <ul>
	 * 
	 * @return the new version of the XPath, or an empty string if a refinement
	 * is not applicable
	 */
	public String finer() {
		XPathTag tag = tags.stream()
				.filter(XPathTag::canIncrement)
				.reduce((__, t) -> t).orElse(null);
		if (tag!=null) {
			tag.increment();
			path = "";
			return get();
		}
		return "";
	}
	
	/**
	 * Produce a more general version of this XPath by removing a tag or attribute
	 * starting from the first tag, so that the resulting new version 
	 * is expected to match a larger collection of links. 
	 * A single invocation of this method will result 
	 * in only one generalization of a tag or attribute. 
	 * If a generalization cannot be applied (i.e. this is the most general XPath) 
	 * an empty string is returned. 
	 * <br>Possible refinements are:<br>
	 * <ul>
	 * <li>Remove an attribute value: <code>//div[@id="page"]/font/a-> //div[@id]/font/a</code></li>
	 * <li>Remove an attribute: <code>//div[@id]/font/a -> //div/font/a</code></li>
	 * <li>Remove a tag: <code>//div/font/a -> //font/a</code></li>
	 * <ul>
	 * 
	 * @return the new version of the XPath, or an empty string if a refinement
	 * is not applicable
	 */
	public String coarser() {
		XPathTag tag = tags.stream()
				.filter(XPathTag::canDecrement)
				.findFirst().orElse(null);
		if (tag!=null) {
			tag.decrement();
			path = "";
			return get();
		}
		return "";
	}
	
	/**
	 * Performs a refinement of this XPath: {@link XPath#finer()} 
	 * if finer is true, else {@link XPath#coarser()} 
	 * @param finer
	 * @return true if a refinement has been applied, false otherwise
	 */
	public boolean refine(boolean finer) {
		String version = (finer) ? finer() : coarser();
		return !version.isEmpty();
	}
	
	
	/**
	 * Returns the most accurate XPath applicable.
	 * @return the finest grain version of this XPath
	 */
	public String finest() {
		tags.forEach(XPathTag::setFinest);
		path = "";
		return get();
	}
	
	
	/**
	 * Returns the most general XPath applicable, i.e. <code>//a</code>
	 * @return the most general version of this XPath
	 */
	public String coarsest() {
		tags.forEach(XPathTag::omit);
		path = "";
		return get();
	}
	
	private void build(DomNode node) {
		tags = new TreeSet<>();
		int index=0;
		boolean idFound = false;
		while (node.getNodeName()!="#document") {
			XPathTag tag = new XPathTag(node.getNodeName(),index);
			//if (index==0) tag.addNodeTest("text()");
			NamedNodeMap attrs = node.getAttributes();
			int n = attrs.getLength();
			for (int i=0; i<=n-1; i++) {
				Node attr = attrs.item(i);
				String name = attr.getNodeName();
				if (index>0 || (!name.equals("href") && !name.contains(":")))
					tag.addAttribute(name, attr.getNodeValue());
			}
			if (!idFound) {
				if (tag.hasId()) {
					tag.setIdOnly();
					idFound = true;
				}
				else if (index==0) tag.setAllAttributes();
				else tag.setFirstAttribute();
			} 
			//else tag.setUse(false); // always use all tags (?)
			tags.add(tag);
			node = node.getParentNode();
			index++;
		}
	}
	
	@Override
	public int hashCode() {
		return get().hashCode();
	}
	
	
	/**
	 * Two XPaths are said to be equal if their 
	 * <i>current version</i> are equal.
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		XPath o = (XPath) obj;
		return Objects.equals(get(), o.get());
	}

}
