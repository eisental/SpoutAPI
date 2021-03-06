/*
 * This file is part of SpoutAPI (http://www.spout.org/).
 *
 * SpoutAPI is licensed under the SpoutDev License Version 1.
 *
 * SpoutAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * SpoutAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.api.util.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

/**
 * YAML configuration loader. To use this class, construct it with path to
 * a file and call its load() method. For specifying node paths in the
 * various get*() methods, they support SK's path notation, allowing you to
 * select child nodes by delimiting node names with periods.
 *
 * <p>
 * For example, given the following configuration file:</p>
 *
 * <pre>members:
 *	 - Hollie
 *	 - Jason
 *	 - Bobo
 *	 - Aya
 *	 - Tetsu
 * worldguard:
 *	 fire:
 *		 spread: false
 *		 blocks: [cloth, rock, glass]
 * sturmeh:
 *	 cool: false
 *	 eats:
 *		 babies: true</pre>
 *
 * <p>Calling code could access sturmeh's baby eating state by using
 * <code>getBoolean("sturmeh.eats.babies", false)</code>. For lists, there are
 * methods such as <code>getStringList</code> that will return a type safe list.
 *
 * <p>This class is currently incomplete. It is not yet possible to get a node.
 * </p>
 *
 * @author sk89q
 */
public class Configuration extends ConfigurationNode {
	private Yaml yaml;
	private File file;
	private String header = null;

	public Configuration(File file) {
		super(new HashMap<String, Object>());

		DumperOptions options = new DumperOptions();

		options.setIndent(4);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		yaml = new Yaml(new SafeConstructor(), new EmptyNullRepresenter(), options);

		this.file = file;
	}

	/**
	 * Loads the configuration file. All errors are thrown away.
	 */
	public void load() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String str;
			StringBuffer buffer = new StringBuffer(10000);
			while ((str = in.readLine()) != null) {
				buffer.append(str.replaceAll("\t", "    "));
				buffer.append('\n');
			}

			read(yaml.load(new StringReader(buffer.toString())));
		} catch (IOException e) {
			root = new HashMap<String, Object>();
		} catch (ConfigurationException e) {
			root = new HashMap<String, Object>();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {}
		}
	}

	/**
	 * Set the header for the file as a series of lines that are terminated
	 * by a new line sequence.
	 *
	 * @param headerLines header lines to prepend
	 */
	public void setHeader(String ... headerLines)  {
		StringBuilder header = new StringBuilder();

		for (String line : headerLines) {
			if (header.length() > 0) {
				header.append("\r\n");
			}
			header.append(line);
		}

		setHeader(header.toString());
	}

	/**
	 * Set the header for the file. A header can be provided to prepend the
	 * YAML data output on configuration save. The header is
	 * printed raw and so must be manually commented if used. A new line will
	 * be appended after the header, however, if a header is provided.
	 *
	 * @param header header to prepend
	 */
	public void setHeader(String header) {
		this.header = header;
	}

	/**
	 * Return the set header.
	 *
	 * @return
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * Saves the configuration to disk. All errors are clobbered.
	 *
	 * @param header header to prepend
	 * @return true if it was successful
	 */
	public boolean save() {
		FileOutputStream stream = null;

		File parent = file.getParentFile();

		if (parent != null) {
			parent.mkdirs();
		}

		try {
			stream = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
			if (header != null) {
				writer.append(header);
				writer.append("\r\n");
			}
			yaml.dump(root, writer);
			return true;
		} catch (IOException e) {} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private void read(Object input) throws ConfigurationException {
		try {
			if (null == input) {
				root = new HashMap<String, Object>();
			} else {
				root = (Map<String, Object>) input;
			}
		} catch (ClassCastException e) {
			throw new ConfigurationException("Root document must be an key-value structure");
		}
	}

	/**
	 * This method returns an empty ConfigurationNode for using as a
	 * default in methods that select a node from a node list.
	 * @return
	 */
	public static ConfigurationNode getEmptyNode() {
		return new ConfigurationNode(new HashMap<String, Object>());
	}
}

class EmptyNullRepresenter extends Representer {
	public EmptyNullRepresenter() {
		super();
		this.nullRepresenter = new EmptyRepresentNull();
	}

	protected class EmptyRepresentNull implements Represent {
		public Node representData(Object data) {
			return representScalar(Tag.NULL, ""); // Changed "null" to "" so as to avoid writing nulls
		}
	}

	// Code borrowed from snakeyaml (http://code.google.com/p/snakeyaml/source/browse/src/test/java/org/yaml/snakeyaml/issues/issue60/SkipBeanTest.java)
	@Override
	protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
		NodeTuple tuple = super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
		Node valueNode = tuple.getValueNode();
		if (valueNode instanceof CollectionNode) {
			// Removed null check
			if (Tag.SEQ.equals(valueNode.getTag())) {
				SequenceNode seq = (SequenceNode) valueNode;
				if (seq.getValue().isEmpty()) {
					return null; // skip empty lists
				}
			}
			if (Tag.MAP.equals(valueNode.getTag())) {
				MappingNode seq = (MappingNode) valueNode;
				if (seq.getValue().isEmpty()) {
					return null; // skip empty maps
				}
			}
		}
		return tuple;
	}
	// End of borrowed code
}
