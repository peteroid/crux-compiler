package crux;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    private static final String[] PREDEFINED_IDENTIFIERS =
            {
                    "readInt",
                    "readFloat",
                    "printBool",
                    "printInt",
                    "printFloat",
                    "println"
            };

    public static boolean isPredefined(String name) {
        for (String str : PREDEFINED_IDENTIFIERS) {
            if (str.equals(name))
                return true;
        }
        return false;
    }

    private Map<String, Symbol> symbolMap;
    protected SymbolTable parent;
    private int depth;
    
    public SymbolTable(SymbolTable parent)
    {
        symbolMap = new LinkedHashMap<String, Symbol>();
        if (parent != null)
        {
            this.parent = parent;
            depth = parent.depth + 1;
        }
        else
        {
            depth = 0;
            for (String s : PREDEFINED_IDENTIFIERS)
                this.insert(s);
        }
    }
    
    public Symbol lookup(String name) throws SymbolNotFoundError
    {
        Symbol s = symbolMap.get(name);
        if (s == null)
        {
            if (parent != null)
                return parent.lookup(name);
            else
                throw new SymbolNotFoundError(name);
        }
        else
        {
            return s;
        }
    }
       
    public Symbol insert(String name) throws RedeclarationError
    {
        Symbol s = symbolMap.get(name);
        if (s == null)
        {
            s = new Symbol(name);
            symbolMap.put(name, s);
            return s;
        }
        else
        {
            throw new RedeclarationError(s);
        }

    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if (parent != null)
            sb.append(parent.toString());
        
        String indent = new String();
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }
        
        for (Symbol s : symbolMap.values())
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
