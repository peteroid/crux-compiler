package types;

public class FuncType extends Type {

    public static FuncType predefinedFunc(String name) {
        TypeList arguments = new TypeList();
        if (name.equals("readInt")) {
            return new FuncType(arguments, new IntType());
        } else if (name.equals("readFloat")) {
            return new FuncType(arguments, new FloatType());
        } else if (name.equals("printBool")) {
            arguments.append(new BoolType());
            return new FuncType(arguments, new VoidType());
        } else if (name.equals("printInt")) {
            arguments.append(new IntType());
            return new FuncType(arguments, new VoidType());
        } else if (name.equals("printFloat")) {
            arguments.append(new FloatType());
            return new FuncType(arguments, new VoidType());
        } else if (name.equals("println")) {
            return new FuncType(arguments, new VoidType());
        }
        return null;
    }

    private TypeList args;
    private Type ret;

    public FuncType(TypeList args, Type returnType)
    {
        this.args = args;
        this.ret = returnType;
    }

    public Type returnType()
    {
        return ret;
    }

    public TypeList arguments()
    {
        return args;
    }

    @Override
    public Type call(Type args)
    {
        if (args instanceof TypeList && !((TypeList) args).containsArrayType()) {
            if (args.equivalent(arguments()))
                return returnType();
        }
        return super.call(args);
    }

    @Override
    public String toString()
    {
        return "func(" + args + "):" + ret;
    }

    @Override
    public boolean equivalent(Type that)
    {
        if (that == null)
            return false;
        if (!(that instanceof FuncType))
            return false;

        FuncType aType = (FuncType)that;
        return this.ret.equivalent(aType.ret) && this.args.equivalent(aType.args);
    }
}
