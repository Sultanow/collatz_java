package collatz.graph;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;

public class CsvGraph extends AbstractCollatzGraph {
	
	private BigInteger[][] successorMap;
	private Map<BigInteger, Node> predecessorMap = new HashMap<>();
	private Map<Node, Node> loopBacks = new HashMap<>();
	private Map<BigInteger, Boolean> prunableMap = new HashMap<>();

	private final String file;
	private final Pattern csvPattern;
	private final int root;

	public CsvGraph(int w, int h, int nodeWidth, int nodeHeight, int hSpacing, int vSpacing, String file,
			Pattern csvPattern, int root, boolean drawSelfLoops, boolean rotate) {
		super(w, h, nodeWidth, nodeHeight, hSpacing, vSpacing, drawSelfLoops, rotate);

		this.file = file;
		this.csvPattern = csvPattern;
		this.root = root;
	}

	protected void init() {
		Node.root = new Node(BigInteger.valueOf(root), null, null);
		grid[0][0] = Node.root;

		File resourcesPath = new File(CsvGraph.class.getClassLoader().getResource(".").getFile());
		Path path = Paths.get(resourcesPath.getAbsolutePath(), file);
		try {
			List<String> lines = Files.readAllLines(path);
			int lineCount = lines.size() - 1;
			successorMap = new BigInteger[lineCount][2];
			int i = 0;
			for (String line : lines) {
				if (i > 0) {
					Matcher matcher = csvPattern.matcher(line);
					if (matcher.matches()) {
						successorMap[i - 1][0] = BigInteger.valueOf(Long.valueOf(matcher.group(1)));
						successorMap[i - 1][1] = BigInteger.valueOf(Long.valueOf(matcher.group(2)));
						prunableMap.put(successorMap[i - 1][1], Boolean.valueOf(matcher.group(3)));
					}
				}
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		int row = 0;
		int col = 0;
		generateSuccessors(Node.root, row, col);
		Node.recalculateColumns();

		// Begin drawing
		Object parent = getDefaultParent();
		getModel().beginUpdate();
		draw(Node.root, parent);
		drawLoopBacks(parent);
		getModel().endUpdate();
	}

	public void drawLoopBacks(Object parent) {
		if (!loopBacks.isEmpty()) {
			this.view.setTranslate(new mxPoint(20, 0));

			Hashtable<String, Object> loopBackEdgeStyle = new Hashtable<String, Object>();
			loopBackEdgeStyle.put(mxConstants.STYLE_EXIT_X, -1);
			loopBackEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 0);
			loopBackEdgeStyle.put(mxConstants.STYLE_ENTRY_X, -1);
			loopBackEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 0);
			loopBackEdgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
			stylesheet.putCellStyle("LOOP_BACK", loopBackEdgeStyle);

			List<Node> selfLoopBacks = new ArrayList<>();
			for (Map.Entry<Node, Node> loopBack : loopBacks.entrySet()) {
				if (loopBack.getKey().equals(loopBack.getValue())) {
					selfLoopBacks.add(loopBack.getKey());
				} else {
					Object edge = insertEdge(parent, loopBack.getKey().value + "_" + loopBack.getValue().value, null,
							loopBack.getKey().vertex, loopBack.getValue().vertex, "edgeStyle=LOOP_BACK");
					((mxCell) edge).setStyle("LOOP_BACK");
				}
			}

			if (drawSelfLoops && !selfLoopBacks.isEmpty()) {
				loopBackEdgeStyle = new Hashtable<String, Object>();
				loopBackEdgeStyle.put(mxConstants.STYLE_EXIT_X, 1);
				loopBackEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 0.23);
				loopBackEdgeStyle.put(mxConstants.STYLE_ENTRY_X, 1);
				loopBackEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 0.77);
				loopBackEdgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_LOOP);
				stylesheet.putCellStyle("SELF_LOOP_BACK", loopBackEdgeStyle);

				for (Node selfLoopBack : selfLoopBacks) {
					Object edge = insertEdge(parent, selfLoopBack.value + "_" + selfLoopBack.value, null,
							selfLoopBack.vertex, selfLoopBack.vertex, "edgeStyle=LOOP_BACK");
					((mxCell) edge).setStyle("SELF_LOOP_BACK");
				}
			}
		}
	}

	private void generateSuccessors(Node node, int row, int col) {
		predecessorMap.put(node.value, node);
		if (row <= this.h) {
			int i = 0;
			for (BigInteger[] key : successorMap) {
				if (key[0].equals(node.value)) {
					Node existing = predecessorMap.get(key[1]);
					if (existing != null) {
						loopBacks.put(node, existing);
					} else {
						Node successor = new Node(key[1], node, Node.root);
						if (prunableMap.containsKey(key[1]))
							successor.prunable = prunableMap.get(key[1]);
						generateSuccessors(successor, row + 1, col + i);
					}
				}
			}
		}
	}
}
