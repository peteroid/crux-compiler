package ast;

import crux.Symbol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeclarationList extends Command implements Iterable<Declaration> {
	
	private List<Declaration> list;
	
	public DeclarationList(int lineNum, int charPos)
	{
		super(lineNum, charPos);
		list = new ArrayList<Declaration>();
	}
	
	public void add(Declaration command)
	{
		list.add(command);
	}


	public List<Symbol> toSymbolList() {
		List<Symbol> symbolList = new ArrayList<Symbol>();
		for (Declaration declaration : list) {
			symbolList.add(declaration.symbol());
		}
		return symbolList;
	}

	@Override
	public Iterator<Declaration> iterator() {
		return list.iterator();
	}

	@Override
	public void accept(CommandVisitor visitor) {
		visitor.visit(this);
	}
}
