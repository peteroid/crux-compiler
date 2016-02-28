package ast;

import crux.Symbol;
import types.Type;
import types.TypeList;

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

	public TypeList toTypeList() {
		TypeList typeList = new TypeList();
		for (Declaration declaration : list) {
			typeList.append(declaration.symbol().type());
		}
		return typeList;
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
