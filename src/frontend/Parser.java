package frontend;

import frontend.exception.SysYException;
import frontend.exception.SysYException.EKind;
import frontend.token.Tokens.*;
import frontend.tree.SysYTree.*;
import utils.Writer;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static final boolean debug = false;

    private final Scanner scanner;
    private Writer writer;

    /** If needed to print syntax.
     */
    private final boolean ifPrint;

    /** List of Errors.
     */
    public final List<SysYException> errors = new ArrayList<>();

    /** The frontend.token, set by nextToken().
     */
    private Token token;

    /** The previous frontend.token, set by nextToken().
     */
    private Token prevToken;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
        ifPrint = false;
    }

    public Parser(Scanner scanner, Writer writer, boolean printTree) {
        this.scanner = scanner;
        this.writer = writer;
        this.ifPrint = printTree;
    }

    /**
     * Main function for syntax analysis.
     */
    public SysYCompilationUnit syntaxAnalyse() throws SysYException {
        nextToken();
        return parseCompilationUnit();
    }

    /*----------------helper functions----------------*/
    private void nextToken() {
        if (token != null && ifPrint) {
            if (debug) System.out.println(token);
            else writer.writeToken(token);
        }
        scanner.nextToken();
        prevToken = token;
        token = scanner.getToken();
    }
    
    private Token lookAhead(int pos) { return scanner.lookAheadToken(pos); }

    private void printTree(String out) {
        if (ifPrint) {
            if (debug) System.out.println(out);
            else writer.writeln(out);
        }
    }

    /** If next input frontend.token matches given frontend.token, skip it, otherwise report
     *  an error.
     */
    public void accept(TokenKind tk) throws SysYException {
        if (token.getTokenKind() == tk) {
            nextToken();
        } else if (tk == TokenKind.SEMI) {
            throw new SysYException(EKind.i, prevToken.getLine());
        } else if (tk == TokenKind.RPAR) {
            throw new SysYException(EKind.j, prevToken.getLine());
        } else if (tk == TokenKind.RSQU) {
            throw new SysYException(EKind.k, prevToken.getLine());
        }
    }

    private boolean isExp() {
        switch (token.getTokenKind()) {
            case LPAR:
            case INTC:
            case IDENT:
            case PLUS: case MINUS: case NOT:
                return true;
            default:
                return false;
        }
    }

    private boolean isFuncType() {
        switch (token.getTokenKind()) {
            case INT: case VOID:
                return true;
            default:
                return false;
        }
    }

    private boolean isUnaryOp() {
        return token.getTokenKind() == TokenKind.NOT || isAddOp();
    }

    private boolean isMulOp() {
        switch (token.getTokenKind()) {
            case STAR: case DIV: case MOD:
                return true;
            default:
                return false;
        }
    }

    private boolean isAddOp() {
        switch (token.getTokenKind()) {
            case PLUS: case MINUS:
                return true;
            default:
                return false;
        }
    }

    private boolean isRelOp() {
        switch (token.getTokenKind()) {
            case GRE: case GEQ:
            case LSS: case LEQ:
                return true;
            default:
                return false;
        }
    }

    private boolean isEqOp() {
        switch (token.getTokenKind()) {
            case EQL: case NEQ:
                return true;
            default:
                return false;
        }
    }

    private boolean isLAndOp() {
        return token.getTokenKind() == TokenKind.AND;
    }

    private boolean isLOrOp() {
        return token.getTokenKind() == TokenKind.OR;
    }

    /*----------------parse functions----------------*/
    /**
     * Begin parse compilation unit.
     */
    public SysYCompilationUnit parseCompilationUnit() throws SysYException {
        SysYCompilationUnit top = new SysYCompilationUnit();
        while (true) {
            if (token.getTokenKind() == TokenKind.CONST) {
                top.addDecl(constDecl());
            } else if (token.getTokenKind() == TokenKind.VOID) {
                top.addFuncDef(funcDef());
            } else if (token.getTokenKind() == TokenKind.INT) {
                if (lookAhead(0).getTokenKind() == TokenKind.MAIN) {
                    top.setMainFuncDef(mainFuncDef());
                    break;
                } else if (lookAhead(0).getTokenKind() == TokenKind.IDENT
                            && lookAhead(1).getTokenKind() == TokenKind.LPAR) {
                    // func def with frontend.token == INT
                    top.addFuncDef(funcDef());
                } else if (lookAhead(0).getTokenKind() == TokenKind.IDENT) {
                    // var declaration with frontend.token == INT
                    top.addDecl(decl());
                } else {
                    throw new SysYException(EKind.o, token.getLine());
                }
            } else {
                throw new SysYException(EKind.o, token.getLine());
            }
        }
        printTree("<CompUnit>");
        return top;
    }

    /**
     * Begin parse const declaration with tokenKind == "const".
     */
    public SysYBlockItem constDecl() throws SysYException {
        List<SysYSymbol> defs = new ArrayList<>();
        accept(TokenKind.CONST);
        accept(TokenKind.INT);

        defs.add(constDef());
        while (token.getTokenKind() == TokenKind.COMMA) {
            nextToken();
            defs.add(constDef());
        }
        try {
            accept(TokenKind.SEMI);
        } catch (SysYException e) {
            errors.add(e);
        }
        printTree("<ConstDecl>");
        return new SysYDecl(true, defs);
    }

    /**
     * Begin parse const definition with tokenKind == identifier.
     */
    public SysYSymbol constDef() throws SysYException {
        SysYIdentifier ident = ident();
        int dimension = 0;
        SysYExpression exp = null, exp2 = null, init = null;
        SysYSymbol def;

        if (token.getTokenKind() == TokenKind.LSQU) {
            nextToken();
            exp = constExp();
            dimension = 1;
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.getTokenKind() == TokenKind.LSQU) {
                nextToken();
                exp2 = constExp();
                dimension = 2;
                try {
                    accept(TokenKind.RSQU);
                } catch (SysYException e) {
                    errors.add(e);
                }
            }
        }

        if (token.getTokenKind() == TokenKind.ASSIGN) {
            nextToken();
            init = constInit();
        }

        printTree("<ConstDef>");
        def = new SysYDef(true, ident, dimension, exp, exp2, init);
        return def;
    }

    /**
     * Begin parse const initializers with frontend.token after '='.
     */
    public SysYExpression constInit() throws SysYException {
        List<SysYExpression> expressions = new ArrayList<>();
        if (token.getTokenKind() == TokenKind.LBRACE) {
            nextToken();
            if (token.getTokenKind() != TokenKind.RBRACE) {
                expressions.add(constInit());
                while (token.getTokenKind() == TokenKind.COMMA) {
                    nextToken();
                    expressions.add(constInit());
                }
            }
            accept(TokenKind.RBRACE);
        } else {
            expressions.add(constExp());
        }
        printTree("<ConstInitVal>");
        return new SysYInit(true, expressions);
    }

    /**
     * Begin parse declaration with tokenKind == INT.
     */
    public SysYDecl decl() throws SysYException {
        List<SysYSymbol> defs = new ArrayList<>();
        accept(TokenKind.INT);

        defs.add(def());
        while (token.getTokenKind() == TokenKind.COMMA) {
            nextToken();
            defs.add(def());
        }
        try {
            accept(TokenKind.SEMI);
        } catch (SysYException e) {
            errors.add(e);
        }
        printTree("<VarDecl>");
        return new SysYDecl(false, defs);
    }

    /**
     * Begin parse definition with tokenKind == identifier.
     */
    public SysYSymbol def() throws SysYException {
        SysYIdentifier ident = ident();
        int dimension = 0;
        SysYExpression exp = null, exp2 = null, init = null;
        SysYSymbol def;

        if (token.getTokenKind() == TokenKind.LSQU) {
            nextToken();
            exp = constExp();
            dimension = 1;
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.getTokenKind() == TokenKind.LSQU) {
                nextToken();
                exp2 = constExp();
                dimension = 2;
                try {
                    accept(TokenKind.RSQU);
                } catch (SysYException e) {
                    errors.add(e);
                }
            }
        }

        if (token.getTokenKind() == TokenKind.ASSIGN) {
            nextToken();
            init = init();
        }

        printTree("<VarDef>");
        def = new SysYDef(false, ident, dimension, exp, exp2, init);
        return def;
    }

    /**
     * Begin parse initializers with frontend.token after '='.
     */
    public SysYExpression init() throws SysYException {
        List<SysYExpression> expressions = new ArrayList<>();
        if (token.getTokenKind() == TokenKind.LBRACE) {
            nextToken();
            if (token.getTokenKind() != TokenKind.RBRACE) {
                expressions.add(init());
                while (token.getTokenKind() == TokenKind.COMMA) {
                    nextToken();
                    expressions.add(init());
                }
            }
            accept(TokenKind.RBRACE);
        } else {
            expressions.add(exp());
        }
        printTree("<InitVal>");
        return new SysYInit(false, expressions);
    }

    /**
     * ident
     */
    public SysYIdentifier ident() {
       Token ident = token;
       if (token.getTokenKind() == TokenKind.IDENT) {
           nextToken();
       }
       return new SysYIdentifier(ident);
    }

    /**
     * FuncType → 'void' | 'int'
     * @return true for return int value
     */
    public boolean funcType() {
        Token type = token;
        if (isFuncType()) {
            nextToken();
            printTree("<FuncType>");
        }
        return type.getTokenKind() == TokenKind.INT;
    }

    /**
     * Begin parse function definition with tokenKind == INT/VOID.
     */
    public SysYFuncDef funcDef() throws SysYException {
        boolean returnInt;
        SysYIdentifier ident;
        List<SysYSymbol> funcParams = new ArrayList<>();
        SysYStatement block;
        SysYFuncDef def;

        returnInt = funcType();
        ident = ident();
        accept(TokenKind.LPAR);

        if (token.getTokenKind() == TokenKind.INT) {
            funcParams = funcFParams();
        }

        try {
            accept(TokenKind.RPAR);
        } catch (SysYException e) {
            errors.add(e);
        }


        block = block();
        def = new SysYFuncDef(returnInt, ident, funcParams, block);

        printTree("<FuncDef>");
        return def;
    }

    /**
     * Begin parse function fake parameters with tokenKind after '(', tokenKind == INT.
     */
    public List<SysYSymbol> funcFParams() throws SysYException {
        List<SysYSymbol> funcParams = new ArrayList<>();
        funcParams.add(funcFParam());
        while (token.getTokenKind() == TokenKind.COMMA) {
            nextToken();
            funcParams.add(funcFParam());
        }
        printTree("<FuncFParams>");
        return funcParams;
    }

    /**
     * Begin parse a function fake parameter with tokenKind == INT.
     */
    public SysYSymbol funcFParam() throws SysYException {
        SysYSymbol param;
        accept(TokenKind.INT);
        SysYIdentifier ident = ident();

        if (token.getTokenKind() == TokenKind.LSQU) {
            nextToken();
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.getTokenKind() == TokenKind.LSQU) {
                nextToken();
                SysYExpression constExp = constExp();
                try {
                    accept(TokenKind.RSQU);
                } catch (SysYException e) {
                    errors.add(e);
                }
                param =  new SysYFuncParam(ident, 2, constExp);
            } else {
                param =  new SysYFuncParam(ident, 1);
            }
        } else {
            param = new SysYFuncParam(ident, 0);
        }

        printTree("<FuncFParam>");
        return param;
    }

    /**
     * Begin parse function definition with tokenKind == MAIN.
     */
    public SysYMainFuncDef mainFuncDef() throws SysYException {
        accept(TokenKind.INT);
        accept(TokenKind.MAIN);
        accept(TokenKind.LPAR);
        try {
            accept(TokenKind.RPAR);
        } catch (SysYException e) {
            errors.add(e);
        }
        SysYStatement block = block();
        printTree("<MainFuncDef>");
        return new SysYMainFuncDef(block);
    }

    /*--------parse statements-------- */
    /**
     * Stmt → LVal '=' Exp ';'
     * 		| [Exp] ';' //有无Exp两种情况
     * 		| Block
     * 		| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * 		| 'while' '(' Cond ')' Stmt
     * 		| 'break' ';'
     * 		| 'continue' ';'
     * 		| 'return' [Exp] ';'
     * 		| LVal '=' 'getint' '(' ')' ';'
     * 		| 'printf' '(' FormatString { ',' Exp } ')' ';'
     * @return SysYStatement
     */
    public SysYStatement statement() throws SysYException {
        SysYStatement statement = null;
        switch (token.getTokenKind()) {
            case LBRACE:{
                statement = block();
                break;
            }
            case IF: {
                nextToken();
                accept(TokenKind.LPAR);
                SysYExpression cond = cond();
                try {
                    accept(TokenKind.RPAR);
                } catch (SysYException e) {
                    errors.add(e);
                }
                SysYStatement thenStmt = statement();
                SysYStatement elseStmt = null;
                if (token.getTokenKind() == TokenKind.ELSE) {
                    nextToken();
                    elseStmt = statement();
                }
                statement = new SysYIf(cond, thenStmt, elseStmt);
                break;
            }
            case WHILE: {
                nextToken();
                accept(TokenKind.LPAR);
                SysYExpression cond = cond();
                try {
                    accept(TokenKind.RPAR);
                } catch (SysYException e) {
                    errors.add(e);
                }

                SysYStatement thenStmt = statement();

                statement = new SysYWhile(cond, thenStmt);
                break;
            }
            case CONTINUE: {
                int line = token.getLine();

                nextToken();
                try {
                    accept(TokenKind.SEMI);
                } catch (SysYException e) {
                    errors.add(e);
                }
                statement = new SysYContinue(line);
                break;
            }
            case BREAK: {
                int line = token.getLine();

                nextToken();
                try {
                    accept(TokenKind.SEMI);
                } catch (SysYException e) {
                    errors.add(e);
                }
                statement = new SysYBreak(line);
                break;
            }
            case RETURN: {
                int line = token.getLine();

                nextToken();
                SysYExpression result = isExp() ? exp() : null;

                try {
                    accept(TokenKind.SEMI);
                } catch (SysYException e) {
                    errors.add(e);
                }
                statement = new SysYReturn(line, result);
                break;
            }
            case SEMI: {
                nextToken();
                statement = new SysYExpressionStatement();
                break;
            }
            case PRINTF: {
                int line = token.getLine();

                nextToken();
                accept(TokenKind.LPAR);
                if (token.getTokenKind() == TokenKind.FORMATS) {
                    String format = token.getValue();
                    nextToken();
                    List<SysYExpression> expressions = new ArrayList<>();
                    while (token.getTokenKind() == TokenKind.COMMA) {
                        nextToken();
                        expressions.add(exp());
                    }

                    statement = new SysYPrintf(line, format, expressions);
                }

                try {
                    accept(TokenKind.RPAR);
                    accept(TokenKind.SEMI);
                } catch (SysYException e) {
                    errors.add(e);
                }
                break;
            }
            default: {
                int pos = 0, line = token.getLine();
                boolean flag = false;
                Token temp;
                while ((temp = lookAhead(pos)).getLine() == line) {
                    pos++;
                    if (temp.getTokenKind() == TokenKind.ASSIGN) {
                        flag = true;
                        break;
                    }
                }

                if (flag) {
                    SysYLVal lVal = (SysYLVal) lVal();
                    accept(TokenKind.ASSIGN);
                    if (token.getTokenKind() == TokenKind.GETINT) {
                        statement = new SysYAssign(lVal, new SysYGetInt());
                        accept(TokenKind.GETINT);
                        accept(TokenKind.LPAR);
                        try {
                            accept(TokenKind.RPAR);
                        } catch (SysYException e) {
                            errors.add(e);
                        }
                    } else {
                        statement = new SysYAssign(lVal, exp());
                    }
                } else {
                    statement = new SysYExpressionStatement(exp());
                }

                try {
                    accept(TokenKind.SEMI);
                } catch (SysYException e) {
                    errors.add(e);
                }
            }
        }
        printTree("<Stmt>");
        return statement;
    }

    /**
     * Begin parse block with frontend.token == '{'.
     */
    public SysYStatement block() throws SysYException {
        List<SysYBlockItem> statements = new ArrayList<>();
        accept(TokenKind.LBRACE);

        while (token.getTokenKind() != TokenKind.RBRACE) {
            if (token.getTokenKind() == TokenKind.CONST) {
                statements.add(constDecl());
            } else if (token.getTokenKind() == TokenKind.INT) {
                statements.add(decl());
            } else {
                statements.add(statement());
            }
        }

        int endLine = token.getLine();
        accept(TokenKind.RBRACE);
        printTree("<Block>");
        return new SysYBlock(statements, endLine);
    }

    /*--------parse expressions--------*/
    public SysYExpression exp() throws SysYException {
        SysYExpression expression = addExp();
        printTree("<Exp>");
        return expression;
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     */
    public SysYExpression lVal() throws SysYException {
        SysYIdentifier ident = ident();
        SysYExpression exp;

        if (token.getTokenKind() == TokenKind.LSQU) {
            nextToken();
            SysYExpression firstExp = exp();
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.getTokenKind() == TokenKind.LSQU) {
                nextToken();
                SysYExpression secondExp = exp();
                try {
                    accept(TokenKind.RSQU);
                } catch (SysYException e) {
                    errors.add(e);
                }
                exp = new SysYLVal(ident, 2, firstExp, secondExp);
            } else {
                exp = new SysYLVal(ident, 1, firstExp);
            }
        } else {
            exp = new SysYLVal(ident, 0);
        }
        printTree("<LVal>");
        return exp;
    }

    /**
     * Number → IntConst
     */
    public SysYExpression number() {
        int value = Integer.parseInt(token.getValue());
        nextToken();
        printTree("<Number>");
        return new SysYIntC(value);
    }

    /**
     * PrimaryExp → '(' Exp ')'
     * 		| LVal
     * 		| Number
     */
    public SysYExpression primaryExp() throws SysYException {
        SysYExpression expression;
        switch (token.getTokenKind()) {
            case LPAR: {
                accept(TokenKind.LPAR);
                expression = exp();
                try {
                    accept(TokenKind.RPAR);
                } catch (SysYException e) {
                    errors.add(e);
                }
                break;
            }
            case INTC: {
                expression = number();
                break;
            }
            default: {
                expression = lVal();
                break;
            }
        }
        printTree("<PrimaryExp>");
        return expression;
    }

    /**
     * FuncRParams → Exp { ',' Exp }
     */
    public List<SysYExpression> funcRParams() throws SysYException {
        List<SysYExpression> funcParams = new ArrayList<>();
        funcParams.add(exp());
        while (token.getTokenKind() == TokenKind.COMMA) {
            nextToken();
            funcParams.add(exp());
        }
        printTree("<FuncRParams>");
        return funcParams;
    }

    /**
     * UnaryOp → '+' | '−' | '!'
     */
    public Token unaryOp() {
        Token op = token;
        if (isUnaryOp()) nextToken();
        printTree("<UnaryOp>");
        return op;
    }

    /**
     * UnaryExp → PrimaryExp
     * 		| Ident '(' [FuncRParams] ')'
     * 		| UnaryOp UnaryExp
     */
    public SysYExpression unaryExp() throws SysYException {
        SysYExpression res;
        if (isUnaryOp()) {
            Token op = unaryOp();
            SysYExpression exp = unaryExp();
            res = new SysYUnaryExp(op, exp);
        } else if (token.getTokenKind() == TokenKind.IDENT && lookAhead(0).getTokenKind() == TokenKind.LPAR) {
            SysYIdentifier ident = ident();
            accept(TokenKind.LPAR);
            List<SysYExpression> funcRParams = new ArrayList<>();

            if (isExp()) {
                funcRParams = funcRParams();
            }

            try {
                accept(TokenKind.RPAR);
            } catch (SysYException e) {
                errors.add(e);
            }
            res = new SysYFuncCall(ident, funcRParams);
        } else {
            res = primaryExp();
        }
        printTree("<UnaryExp>");
        return res;
    }

    /**
     * MulExp → UnaryExp {('*' | '/' | '%') UnaryExp}
     */
    public SysYExpression mulExp() throws SysYException {
        SysYExpression temp = unaryExp();
        SysYExpression res = new SysYMulExp(temp);
        printTree("<MulExp>");
        while (isMulOp()) {
            Token prevToken = token;
            nextToken();
            temp = unaryExp();
            res = new SysYMulExp(prevToken, res, temp);
            printTree("<MulExp>");
        }
        return res;
    }

    /**
     * AddExp → MulExp {('+' | '−') MulExp}
     */
    public SysYExpression addExp() throws SysYException {
        SysYExpression temp = mulExp();
        SysYExpression res = new SysYAddExp(temp);
        printTree("<AddExp>");
        while (isAddOp()) {
            Token prevToken = token;
            nextToken();
            temp = mulExp();
            res = new SysYAddExp(prevToken, res, temp);
            printTree("<AddExp>");
        }
        return res;
    }

    /**
     * RelExp → AddExp {('<' | '>' | '<=' | '>=') AddExp}
     */
    public SysYExpression relExp() throws SysYException {
        SysYExpression temp = addExp();
        SysYExpression res = new SysYRelExp(temp);
        printTree("<RelExp>");
        while (isRelOp()) {
            Token prevToken = token;
            nextToken();
            temp = addExp();
            res = new SysYRelExp(prevToken, res, temp);
            printTree("<RelExp>");
        }
        return res;
    }

    /**
     * EqExp → RelExp {('==' | '!=') RelExp}
     */
    public SysYExpression eqExp() throws SysYException {
        SysYExpression temp = relExp();
        SysYExpression res = new SysYEqExp(temp);
        printTree("<EqExp>");
        while (isEqOp()) {
            Token prevToken = token;
            nextToken();
            temp = relExp();
            res = new SysYEqExp(prevToken, res, temp);
            printTree("<EqExp>");
        }
        return res;
    }

    /**
     * LAndExp → EqExp {'&&' EqExp}
     */
    public SysYExpression lAndExp() throws SysYException {
        SysYExpression temp = eqExp();
        SysYExpression res = new SysYLAndExp(temp);
        printTree("<LAndExp>");
        while (isLAndOp()) {
            nextToken();
            temp = eqExp();
            res = new SysYLAndExp(res, temp);
            printTree("<LAndExp>");
        }
        return res;
    }

    /**
     * LOrExp → LAndExp {'||' LAndExp}
     */
    public SysYExpression lOrExp() throws SysYException {
        SysYExpression temp = lAndExp();
        SysYExpression res = new SysYLOrExp(temp);
        printTree("<LOrExp>");
        while (isLOrOp()) {
            nextToken();
            temp = lAndExp();
            res = new SysYLOrExp(res, temp);
            printTree("<LOrExp>");
        }
        return res;
    }

    /**
     * Cond → LOrExp
     */
    public SysYExpression cond() throws SysYException {
        SysYExpression cond = lOrExp();
        printTree("<Cond>");
        return new SysYCond(cond);
    }

    public SysYExpression constExp() throws SysYException {
        SysYExpression expression = addExp();
        printTree("<ConstExp>");
        return expression;
    }
}
