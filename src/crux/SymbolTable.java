package crux;

import java.util.LinkedList;
import java.util.Map;

public class SymbolTable {

    private LinkedList<Map<String, Symbol>> mapList;
    private SymbolTable parent;
    
    public SymbolTable()
    {
        mapList = new LinkedList<Map<String, Symbol>>();
    }
    
    public Symbol lookup(String name) throws SymbolNotFoundError
    {
        throw new RuntimeException("implement this");
    }
       
    public Symbol insert(String name) throws RedeclarationError
    {
        throw new RuntimeException("implement this");
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if (/*I have a parent table*/)
            sb.append(parent.toString());
        
        String indent = new String();
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }
        
        for (/*Every symbol, s, in this table*/)
        {
            sb.append(indent + s.toString() + "\n");
        }
        return sb.toString();
    }
}

class SymbolNotFoundError extends Error
{
    private static final long serialVersionUID = 1L;
    private String name;
    
    SymbolNotFoundError(String name)
    {
        this.name = name;
    }
    
    public String name()
    {
        return name;
    }
}

class RedeclarationError extends Error
{
    private static final long serialVersionUID = 1L;

    public RedeclarationError(Symbol sym)
    {
        super("Symbol " + sym + " being redeclared.");
    }
}
