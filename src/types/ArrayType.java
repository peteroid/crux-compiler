package types;

public class ArrayType extends Type {
    
    private Type base;
    private int extent;
    
    public ArrayType(int extent, Type base)
    {
        this.extent = extent;
        this.base = base;
    }
    
    public int extent()
    {
        return extent;
    }
    
    public Type base()
    {
        return base;
    }

    @Override
    public Type deref()
    {
        return this;
    }

    @Override
    public Type index(Type that)
    {
        if(!(that instanceof IntType))
            super.index(that);
        return base.deref();
    }

    public Type isInvalid() {
        if (base instanceof ErrorType || base instanceof VoidType) {
            return base;
        } else if (base instanceof ArrayType) {
            return ((ArrayType) base()).isInvalid();
        }

        return null;
    }

    @Override
    public String toString()
    {
        return "array[" + extent + "," + base + "]";
    }
    
    @Override
    public boolean equivalent(Type that)
    {
        if (that == null)
            return false;
        if (!(that instanceof IntType))
            return false;
        
        ArrayType aType = (ArrayType)that;
        return this.extent() == aType.extent && base.equivalent(aType.base);
    }
}
