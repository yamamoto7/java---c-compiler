import java.io.*;
import java.util.*;

public class Cmpi{
    static BufferedReader source;
    static int line_number;
    static char ch;
    static String id_string;
    static int literal_value;

    static final boolean debug_parse=false;

    static Map<String, id_record> symbol_table;
    static int variable_count = 0;

    static int memory[];
    static Scanner sc;

    public static void main(String[] args) throws Exception{
	if(args.length==1)
	    source = new BufferedReader(new FileReader(new File(args[0])));
	else{
	    source = new BufferedReader(new InputStreamReader(System.in));
	    if(args.length!=0)
		error("multiple source file is not suppoted");
	}
	line_number = 1;
	ch = ' ';
	init_symbol_table();

	/*
	while(sy!=token.END_PROGRAM)
	    get_token();
	*/
	
	get_token();
	tree root = statement();
	if(sy != token.END_PROGRAM)
	    error("text remain");
	//	root.print_statement();
	


	init_interpreter();
	root.execute();
    }

    // 1 statement
    static tree statement(){
	tree p = new tree();
	if(sy == token.SEMICOLON){
	    polish("\n");
	    get_token();
	    p.t = node.Empty_Stmt;
	}
	else if(sy == token.IF){
	    get_token();
	    if(sy != token.LEFT_PAREN)
		error("left parenthesis expected");
	    else{
		get_token();
		polish("(");
	    }

	    // debug
	    p.t = node.If_Stmt;
	    p.a = expression();
	    //	    expression();
	    if(sy != token.RIGHT_PAREN)
		error("right parenthesis expected");
	    else{
		get_token();
		polish(")");
	    }
	    p.b = statement();
	    if(sy == token.ELSE){
		get_token();
		p.c = statement();
	    }
	}
	else if(sy == token.WHILE){
	    p.t = node.While_Stmt;
	    get_token();
	    if(sy != token.LEFT_PAREN)
		error("left parenthesis expected");
	    else
		get_token();
	    // debug
	    p.a = expression();
	    //	    expression();
	    if(sy != token.RIGHT_PAREN)
		error("right parentheis expected");
	    else
		get_token();
	    p.b = statement();
	}
	else if(sy == token.LEFT_BRACE){
	    get_token();
	    int n = 0;
	    if(sy == token.RIGHT_BRACE)
		p.t = node.Empty_Stmt;
	    while(sy != token.RIGHT_BRACE){
		if(sy == token.END_PROGRAM){
		    error("too few right braces at end of statement list");
		    break;
		}
		if(n == 0)
		    p = statement();
		else
		    p = new tree(node.Stmt_Seq,p,statement());
		n++;
	    }
	    get_token();
	}
	else{
	    // debug
	    p.t = node.Expr_Stmt;
	    p.a = expression();
	    //	    expression();
	    if(sy != token.SEMICOLON)
		error("semicolon expected");
	    else{
		polish("\n");
		get_token();
	    }
	}
	return p;
    }

    // 2 expression
    static tree expression(){
	tree p = logical_or_expression();
	if(sy == token.EQUAL){
	    get_token();
	    if(p.t != node.Variable)
		error("error");
	    p = new tree(node.Assign, p, expression());
	    polish("=");
	}
	return (p);
    }

    // 3 log or
    static tree logical_or_expression(){
	tree p = logical_and_expression();
	while(sy == token.OROR){
	    get_token();
	    p = new tree(node.Or_Or, p, logical_and_expression());
	    polish("||");
	}
	return (p);
    }

    // 4 log and
    static tree logical_and_expression(){
	tree p = bit_or_expression();
	while(sy == token.ANDAND){
	    get_token();
	    p = new tree(node.And_And, p, bit_or_expression());
	    polish("&&");
	}
	return (p);
    }

    // 5 bit or
    static tree bit_or_expression(){
	tree p = bit_and_expression();
	while(sy == token.OR){
	    get_token();
	    p = new tree(node.Or_Op, p, bit_and_expression());
	    polish("|");
	}
	return (p);
    }

    // 6 bit and
    static tree bit_and_expression(){
	tree p = equality_expression();
	while(sy == token.AND){
	    get_token();
	    p = new tree(node.And_Op, p, equality_expression());
	    polish("&");
	}
	return(p);
    }

    // 7 touka
    static tree equality_expression(){
	tree p = relational_expression();
	while((sy == token.EQEQ) || (sy == token.NOTEQ)){
	    if(sy == token.EQEQ){
		get_token();
		p = new tree(node.Eq_Op, p, relational_expression());
		polish("==");
	    }
	    while(sy == token.NOTEQ){
		get_token();
		p = new tree(node.Ne_Op, p, relational_expression());
		polish("!=");
	    }
	}
	return (p);
    }
    // 8 kankei
    static tree relational_expression(){
	tree p = additive_expression();
	while((sy == token.GT) || (sy == token.LT) || (sy == token.GE) || (sy == token.LE)){
	    if(sy == token.GT){
		get_token();
		p = new tree(node.Gt_Op, p, additive_expression());
		polish(">");
	    }
	    else if(sy == token.LT){
		get_token();
		p = new tree(node.Lt_Op, p, additive_expression());
		polish("<");
	    }
	    else if(sy == token.GE){
		get_token();
		p = new tree(node.Ge_Op, p, additive_expression());
		polish(">=");
	    }
	    else if(sy == token.LE){
		get_token();
		p = new tree(node.Le_Op, p, additive_expression());
		polish("<=");
	    }
	}
	return (p);
    }
    
    // 9 add
    static tree additive_expression(){
	tree p = multiplicative_expression();
	while((sy == token.PLUS) || (sy == token.MINUS)){
	    if(sy == token.PLUS){
		get_token();
		p = new tree(node.Add, p, multiplicative_expression());
		polish("+");
	    }
	    else if(sy == token.MINUS){
		get_token();
		p = new tree(node.Subtract, p, multiplicative_expression());
		polish("-");
	    }
	}	
	return (p);
    }

    // 10 multiple
    static tree multiplicative_expression(){
	tree p = unary_expression();
	while((sy == token.STAR) || (sy == token.SLASH) || (sy == token.PERCENT)){
	    if(sy == token.STAR){
		get_token();
		p = new tree(node.Multiply, p, unary_expression());
		polish("*");
	    }
	    else if(sy == token.SLASH){
		get_token();
		p = new tree(node.Divide, p, unary_expression());
		polish("/");
	    }
	    else if(sy == token.PERCENT){
		get_token();
		p = new tree(node.Modulo, p, unary_expression());
		polish("%");
	    }
	}
	return (p);
    }
    
    // 11 unary
    static tree unary_expression(){
	if(sy == token.MINUS){
	    get_token();
	    tree p = unary_expression();
	    tree q = new tree(node.Constant, 0);
	    polish("u-");
	    return (new tree(node.Subtract, q, p));
	}
	else{
	    tree p = primary_expression();
	    return p;
	}
    }

    // 12 primary
    static tree primary_expression(){

	if(sy == token.LITERAL){
	    get_token();
	    return (new tree(node.Constant, literal_value));
	}
	else if(sy == token.IDENTIFIER){

	    polish(id_string);
	    get_token();
	    if(sy == token.LEFT_PAREN){
		String id_string_name = new String(id_string);
		id_record x = lookup_function(id_string);
		get_token();
		int count = 0;
		tree a = new tree(node.Constant, 0);
		tree b = new tree(node.Constant, 0);
		a = null;
		b = null;
		if(sy != token.RIGHT_PAREN){
		    a = expression();
		    count++;
		    
		    while(sy == token.COMMA){
			get_token();
			b = expression();
			count++;
		    }
		    if(sy != token.RIGHT_PAREN){
			error("right parrenthesis expexted12-1");
		    }else
			get_token();
		}
		else{
		    get_token();
		}
		if(x.parameter_count != count)
		    error("wrong argument number");
		polish("call-"+count);
		return (new tree(node.Call, x.function_id, id_string_name, a, b));
	    }else{
		id_record x = lookup_variable(id_string);
		return (new tree(node.Variable, x.address, id_string));
	    }
	}
	else if(sy == token.LEFT_PAREN){
	    get_token();
	    tree p = expression();
	    if(sy == token.RIGHT_PAREN){
		get_token();
	    }
	    else
		error("right parenthesis expected12-2");
	    return (p);
	}
	else{
	    error("unrecognized element in expression");
	    get_token();
	    return (new tree(node.Constant, 0));
	}
    }

    // polish
    static void polish(String s){
	if(debug_parse)
	    System.out.print(s+" ");
    }
    
    static void get_token(){
	/*1*/
	if(ch==' '||ch=='\n'||ch=='\t'){
	    next_ch();
	    while(ch==' '||ch=='\n'||ch=='\t')
		next_ch();
	}
	/*2*/
	if(ch==65535){
	    sy=token.END_PROGRAM;
	    // System.out.println("END_PROGRAM");
	    return;
	}
	
	/*3-1*/
	if(('A'<=ch&&ch<='Z') || ch=='_' || ('a'<=ch&&ch<='z')){

	    /* ---------------------------------- */
	    //System.out.println("str test");

	    id_string="";
	    while(('A'<=ch&&ch<='Z') || ch=='_' || ('a'<=ch&&ch<='z') || (('0'<=ch)&&(ch<='9'))){
		id_string+=ch;
		next_ch();
	    }
	    if(id_string.equals("if")){
		sy=token.IF;
		// System.out.println("IF");
	    }
	    else if(id_string.equals("else")){
		sy=token.ELSE;
		// System.out.println("ELSE");
	    }
	    else if(id_string.equals("while")){
		sy=token.WHILE;
		// System.out.println("WHILE");
	    }
	    else{
		sy=token.IDENTIFIER;
		// System.out.println("IDENTIFIER "+id_string);
	    }
	    return;
	}
	/*3-2*/
	else if('0'<=ch&&ch<='9'){

	    /* ---------------------------------- */
	    //System.out.println("num test");

	    int v=ch-'0';
	    next_ch();
	    while(('0'<=ch)&&(ch<='9')){
		v=v*10+ch-'0';
		next_ch();
		if(!('0'<=ch)&&(ch<='9'))
		    break;
		if((v>214748364) || ((v==214748364)&&('7'<ch&&ch<'9'))){
		    while(('0'<=ch)&&(ch<='9'))
			next_ch();
		    error("Too large literal");
		    v=0;
		}
	    }
	    // System.out.println("LITERAL "+v);
	    sy=token.LITERAL;
	    literal_value = v;
	    polish(String.valueOf(literal_value));
	    return;
	}
	/*3-3*/
	else if(ch=='('){

	    /* ---------------------------------- */
	    //System.out.println("( test");

	    next_ch();
	    sy=token.LEFT_PAREN;
	    // System.out.println("LEFT_PAREN");
	    return;
	}
	else if(ch==')'){

	    /* ---------------------------------- */
	    //System.out.println(") test");

	    next_ch();
	    sy=token.RIGHT_PAREN;
	    // System.out.println("RIGHT_PAREN");
	    return;
	}
	else if(ch=='{'){

	    /* ---------------------------------- */
	    //System.out.println("{ test");

	    next_ch();
	    sy=token.LEFT_BRACE;
	    // System.out.println("LEFT_BRACE");
	    return;
	}
	else if(ch=='}'){

	    /* ---------------------------------- */
	    //System.out.println("} test");

	    next_ch();
	    sy=token.RIGHT_BRACE;
	    // System.out.println("RIGHT_BRACE");
	    return;
	}
	else if(ch==','){

	    /* ---------------------------------- */
	    //System.out.println(", test");

	    next_ch();
	    sy=token.COMMA;
	    // System.out.println("COMMA");
	    return;
	}

	else if(ch==';'){

	    /* ---------------------------------- */
	    //System.out.println("; test");

	    next_ch();
	    sy=token.SEMICOLON;
	    // System.out.println("SEMICOLON");
	    return;
	}
	else if(ch=='&'){
	    next_ch();
	    if(ch=='&'){
		next_ch();
		sy=token.ANDAND;
		// System.out.println("ANDAND");
		return;
	    }
	    sy=token.AND;
	    // System.out.println("AND");
	    return;
	}
	else if(ch=='|'){
	    next_ch();
	    if(ch=='|'){
		next_ch();
		sy=token.OROR;
		// System.out.println("OROR");
		return;
	    }
	    sy=token.OR;
	    // System.out.println("OR");
	    return;
	}
	else if(ch=='='){

	    /* ---------------------------------- */
	    //System.out.println("= test");

	    next_ch();
	    if(ch=='='){
		next_ch();
		sy=token.EQEQ;
		// System.out.println("EQEQ");
		return;
	    }
	    sy=token.EQUAL;
	    // System.out.println("EQUAL");
	    return;
	}
	else if(ch=='!'){

	    /* ---------------------------------- */
	    //System.out.println("! test");

	    next_ch();
	    if(ch=='='){
		next_ch();
		sy=token.NOTEQ;
		// System.out.println("NOTEQ");
		return;
	    }
	    error("undefined");
	}
	else if(ch=='<'){

	    /* ---------------------------------- */
	    //System.out.println("< test");

	    next_ch();
	    if(ch=='='){
		next_ch();
		sy=token.LE;
		// System.out.println("LE");
		return;
	    }
	    sy=token.LT;
	    // System.out.println("LT");
	    return;
	}
	else if(ch=='>'){

	    /* ---------------------------------- */
	    //System.out.println("> test");

	    next_ch();
	    if(ch=='='){
		next_ch();
		sy=token.GE;
		// System.out.println("GE");
		return;
	    }
	    sy=token.GT;
	    // System.out.println("GT");
	    return;
	}
	else if(ch=='+'){

	    /* ---------------------------------- */
	    //System.out.println("+ test");

	    next_ch();
	    sy=token.PLUS;
	    // System.out.println("PLUS");
	    return;
	}
	else if(ch=='-'){

	    /* ---------------------------------- */
	    //System.out.println("- test");

	    next_ch();
	    sy=token.MINUS;
	    // System.out.println("MINUS");
	    return;
	}
	else if(ch=='*'){

	    /* ---------------------------------- */
	    //System.out.println("* test");

	    next_ch();
	    sy=token.STAR;
	    // System.out.println("STAR");
	    return;
	}
	else if(ch=='%'){

	    /* ---------------------------------- */
	    //System.out.println("% test");

	    next_ch();
	    sy=token.PERCENT;
	    // System.out.println("PERCENT");
	    return;
	}

	/*3-4*/
	else if(ch=='/'){
	    next_ch();
	    if(ch=='*'){

		/* ---------------------------------- */
		//System.out.println("com start test");
		
		while(ch!=65535){
		    next_ch();

		    /* ---------------------------------- */
		    //System.out.println("com test");

		    if(ch=='*'){
			while(ch=='*'){
			    next_ch();

				/* ---------------------------------- */
				//System.out.println("com end 1 test");

			    if(ch=='/'){

				/* ---------------------------------- */
				//System.out.println("com end 2 test");

				next_ch();
				get_token();
				return;
			    }
			}
		    }
		}
		error("non-terminated comment");
	    }else{
		
		/* ---------------------------------- */
		//System.out.println("/ test");
		
		sy=token.SLASH;
		// System.out.println("SLASH");
		return;
	    }
	}
	/*3-5*/
	else{

	    next_ch();
	    error("unrecoginized charactor");
	}
    }

    static void next_ch(){
	try {
	    ch = (char)source.read();
	    if (ch=='\n')
		line_number++;
	}
	catch(Exception e){
	    System.out.println("IO error occured");
	    System.exit(1);
	}
    }

    static enum token{
	END_PROGRAM,
	IDENTIFIER,
	LITERAL,
	ELSE, IF, WHILE,
	COMMA, SEMICOLON,
	LEFT_BRACE, RIGHT_BRACE,
	LEFT_PAREN, RIGHT_PAREN,
	EQUAL, OROR, ANDAND, OR, AND,
	EQEQ, NOTEQ, LE, LT, GE, GT,
	PLUS, MINUS, STAR, SLASH, PERCENT
    }
    static token sy=token.ELSE;


    // kigouhyou

    static enum type{Variable, Function};

    static class id_record{
	type id_class;
	int address;
	int function_id;
	int parameter_count;

	id_record(type a, int b, int c, int d){
	    this.id_class = a;
	    this.address = b;
	    this.function_id = c;
	    this.parameter_count = d;
	}
    }


    // initialize

    static void init_symbol_table(){
	symbol_table = new TreeMap<String, id_record>();
	variable_count = 0;
	id_record w = new id_record(type.Function, -1, 0, 0);
	symbol_table.put("getd", w);
	id_record x = new id_record(type.Function, -1, 1, 2);
	symbol_table.put("putd", x);
	id_record y = new id_record(type.Function, -1, 2, 0);
	symbol_table.put("newline",y);
	id_record z = new id_record(type.Function, -1, 3, 1);
	symbol_table.put("putchar", z);
    }

    static void init_interpreter(){
	memory = new int[variable_count];
	sc = new Scanner(System.in);
    }

    static id_record search(String name){
	id_record x = symbol_table.get(name);
	if(x != null){
	    return x;
	}else{
	    id_record y = new id_record(type.Variable, variable_count, -1, -1);
	    variable_count++;
	    symbol_table.put(name, y);
	    return y;
	}
    }

    static id_record lookup_variable(String name){
	id_record x = search(name);
	if(x.id_class != type.Variable)
	    error("can't use variable name as function");
	return x;
    }

    static id_record lookup_function(String name){
	id_record x = search(name);
	if(x.id_class != type.Function)
	    error("can't use function name as variable");
	return x;

    }

    // data tree

    static enum node{
	Constant, Variable, Call,
	Assign,
	Multiply, Divide, Modulo, Add, Subtract, And_Op, Or_Op,
	And_And, Or_Or,
	Eq_Op, Ne_Op, Le_Op, Lt_Op, Ge_Op, Gt_Op,
	Empty_Stmt, Expr_Stmt, If_Stmt, While_Stmt, Stmt_Seq
    }

    static class tree{
	node t;
	int v;
	String s;
	tree a, b, c;

	tree (){
	    this.t = null;
	    this.v = 0;
	    this.s = null;
	    this.a = null;
	    this.b = null;
	    this.c = null;
	}

	tree(node t, int v){
	    this.t = t;
	    this.v = v;
	    this.s = null;
	    this.a = null;
	    this.b = null;
	    this.c = null;
	}

	tree(node t, int v, String s){
	    this.t = t;
	    this.v = v;
	    this.s = s;
	    this.a = null;
	    this.b = null;
	    this.c = null;
	}

	tree(node t, tree a, tree b){
	    this.t = t;
	    this.v = 0;
	    this.s = null;
	    this.a = a;
	    this.b = b;
	    this.c = null;
	}

	tree(node t, int v, String s,  tree a, tree b){
	    this.t = t;
	    this.v = v;
	    this.s = s;
	    this.a = a;
	    this.b = b;
	    this.c = null;
	}

	void print_expression(){
	    switch(this.t){
	    case Constant:
		System.out.print("C["+this.v+"]");
		break;
	    case Variable:
		System.out.print("V["+this.s+":"+this.v+"]");
		break;
	    case Call:
		System.out.print("Call["+this.s+":"+this.v+"](");
		if(this.a!=null)
		    this.a.print_expression();
		if(this.b!=null){
		    System.out.print(",");
		    this.b.print_expression();
		}
		System.out.print(")");
		break;
	    case Assign:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Or_Or:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case And_And:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Or_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case And_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Eq_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Ne_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Gt_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Lt_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Ge_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Le_Op:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Add:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Subtract:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Multiply:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Divide:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    case Modulo:
		System.out.print(this.t+"(");
		this.a.print_expression();
		System.out.print(",");
		this.b.print_expression();
		System.out.print(")");
		break;
	    default:
		error(this.t + "Unknown node type in print_expression");
	    }
	}
	void print_statement(){
	    switch(this.t){
	    case Empty_Stmt:
		System.out.println(";");
		break;
	    case Expr_Stmt:
		this.a.print_expression();
		System.out.println();
		break;
	    case If_Stmt:
		System.out.print("If(");
		if(this.a!=null){
		    this.a.print_expression();
		}
		System.out.println(")");
		if(this.b!=null){
		    this.b.print_statement();
		}
		System.out.println("Else");
		if(this.c!=null){
		    this.c.print_statement();
		}else{
		    System.out.println(";");
		}
		System.out.println("End-If");
		break;
	    case While_Stmt:
		System.out.print("While(");
		this.a.print_expression();
		System.out.println(")");
		this.b.print_statement();
		System.out.println("End-While");
		break;
	    case Stmt_Seq:
		tree p = this;
		if(p.b==null){
		    System.out.println(";");
		}
		else if(p.a != null){
		    p.a.print_statement();
		    p.b.print_statement();
		}
		
		break;
	    default:
		run_error("error in print_statement");
		break;
	    }
	}
	void execute(){
	    switch(this.t){
	    case Empty_Stmt:
		return;
	    case Expr_Stmt:
		this.a.evaluate();
		return;
	    case If_Stmt:
		if(this.a.evaluate()!=0)
		    this.b.execute();
		else if(this.c!=null)
		    this.c.execute();
		return;
	    case While_Stmt:
		while(this.a.evaluate()!=0)
		    this.b.execute();
		return;
	    case Stmt_Seq:
		this.a.execute();
		this.b.execute();
		return;
	    default:
		run_error(this.t+": Unknown node type in execute");
		return;
	    }
	}

	int evaluate(){
	    switch(this.t){
	    case Constant:
		return(this.v);
	    case Variable:
		return(memory[this.v]);
	    case Call:
		switch(this.v){
		case 0:
		    System.out.print("getd: ");
		    return(sc.nextInt());
		case 1:
		    int val = this.a.evaluate();
		    String s = String.format("%d",val);
		    int d = this.b.evaluate()-s.length();
		    while(d>0){
			System.out.print(" ");
			d--;
		    }
		    System.out.print(s);
		    return val;
		case 2:
		    System.out.println();
		    return 0;
		case 3:
		    int code = this.a.evaluate();
		    System.out.print((char)code);
		    return code;
		default:
		    run_error("aa");
		    return 0;
		}
	    case Assign:
		int v = this.b.evaluate();
		memory[this.a.v] = v;
		return(this.v);
	    case Multiply:
		return(this.a.evaluate()*this.b.evaluate());
	    case Divide:
		if(this.b.evaluate()==0)
		    run_error("can't devide by 0");
		else
		    return(this.a.evaluate()/this.b.evaluate());
	    case Modulo:
		if(this.b.evaluate()==0)
		    run_error("can't devide by 0");
		else
		return(this.a.evaluate()%this.b.evaluate());
	    case Add:
		return(this.a.evaluate()+this.b.evaluate());
	    case Subtract:
		return(this.a.evaluate()-this.b.evaluate());
	    case And_Op:
		return(this.a.evaluate() & this.b.evaluate());
	    case Or_Op:
		return(this.a.evaluate() | this.b.evaluate());
	    case And_And:
		if(this.a.evaluate() != 0 && this.b.evaluate() != 0)
		    return 1;
		else
		    return 0;
	    case Or_Or:
		if(this.a.evaluate() != 0 || this.b.evaluate() != 0)
		    return 1;
		else
		    return 0;
	    case Eq_Op:
		if(this.a.evaluate() == this.b.evaluate())
		    return 1;
		else
		    return 0;
	    case Ne_Op:
		if(this.a.evaluate() != this.b.evaluate())
		    return 1;
		else
		    return 0;
	    case Le_Op:
		if(this.a.evaluate() <= this.b.evaluate())
		    return 1;
		else
		    return 0;
	    case Lt_Op:
		if(this.a.evaluate() < this.b.evaluate())
		    return 1;
		else
		    return 0;
	    case Ge_Op:
		if(this.a.evaluate() >= this.b.evaluate())
		    return 1;
		else
		    return 0;
	    case Gt_Op:
		if(this.a.evaluate() > this.b.evaluate())
		    return 1;
		else
		    return 0;
	    default:
		run_error(this.t+": Unknown node type in evaluate");
		return 0;
	    }
	}
}



    // output error

    static void error(String s){
	System.out.println(String.format("%4d",line_number)+": "+s);
    }
    static void run_error(String s){
	System.out.println(s);
	System.exit(1);
    }
}
