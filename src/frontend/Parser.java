package frontend;

import frontend.exception.SysYException;
import frontend.exception.SysYException.EKind;
import frontend.token.Tokens.*;
import frontend.tree.SysYTree.*;
import io.Writer;

import java.io.IOException;
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
        if (token.tokenKind == tk) {
            nextToken();
        } else if (tk == TokenKind.SEMI) {
            throw new SysYException(EKind.i, prevToken.line);
        } else if (tk == TokenKind.RPAR) {
            throw new SysYException(EKind.j, prevToken.line);
        } else if (tk == TokenKind.RSQU) {
            throw new SysYException(EKind.k, prevToken.line);
        } else {
            throw new SysYException(EKind.o, token.line);
        }
    }

    public boolean isFuncType() {
        return token.getTokenKind() == TokenKind.INT || token.getTokenKind() == TokenKind.VOID;
    }

    public boolean isUnaryOp() {
        return token.tokenKind == TokenKind.NOT || isAddOp();
    }

    public boolean isMulOp() {
        return token.tokenKind == TokenKind.STAR || token.tokenKind == TokenKind.DIV
                || token.tokenKind == TokenKind.MOD;
    }

    public boolean isAddOp() {
        return token.tokenKind == TokenKind.PLUS || token.tokenKind == TokenKind.MINUS;
    }

    public boolean isRelOp() {
        return token.tokenKind == TokenKind.GRE || token.tokenKind == TokenKind.LSS
                || token.tokenKind == TokenKind.GEQ || token.tokenKind == TokenKind.LEQ;
    }

    public boolean isEqOp() {
        return token.tokenKind == TokenKind.EQL || token.tokenKind == TokenKind.NEQ;
    }

    public boolean isLAndOp() {
        return token.tokenKind == TokenKind.AND;
    }

    public boolean isLOrOp() {
        return token.tokenKind == TokenKind.OR;
    }

    /*----------------parse functions----------------*/
    /**
     * Begin parse compilation unit.
     */
    public SysYCompilationUnit parseCompilationUnit() throws SysYException {
        SysYCompilationUnit top = new SysYCompilationUnit();

        while (true) {
            if (token.tokenKind == TokenKind.CONST) {
                top.addDecl(constDecl());
            } else if (token.tokenKind == TokenKind.VOID) {
                top.addFuncDef(funcDef());
            } else if (token.tokenKind == TokenKind.INT) {
                if (lookAhead(0).tokenKind == TokenKind.MAIN) {
                    nextToken();
                    top.setMainFuncDef(mainFuncDef());
                    break;
                } else if (lookAhead(0).tokenKind == TokenKind.IDENT
                            && lookAhead(1).tokenKind == TokenKind.LPAR) {
                    // func def with frontend.token == INT
                    top.addFuncDef(funcDef());
                } else if (lookAhead(0).tokenKind == TokenKind.IDENT) {
                    // var declaration with frontend.token == INT
                    top.addDecl(decl());
                } else {
                    throw new SysYException(EKind.o, token.line);
                }
            } else {
                throw new SysYException(EKind.o, token.line);
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
        while (token.tokenKind == TokenKind.COMMA) {
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

        if (token.tokenKind == TokenKind.LSQU) {
            nextToken();
            exp = constExp();
            dimension = 1;
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.tokenKind == TokenKind.LSQU) {
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

        if (token.tokenKind == TokenKind.ASSIGN) {
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
        if (token.tokenKind == TokenKind.LBRACE) {
            nextToken();
            if (token.tokenKind != TokenKind.RBRACE) {
                expressions.add(constInit());
                while (token.tokenKind == TokenKind.COMMA) {
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
        while (token.tokenKind == TokenKind.COMMA) {
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

        if (token.tokenKind == TokenKind.LSQU) {
            nextToken();
            exp = constExp();
            dimension = 1;
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.tokenKind == TokenKind.LSQU) {
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

        if (token.tokenKind == TokenKind.ASSIGN) {
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
        if (token.tokenKind == TokenKind.LBRACE) {
            nextToken();
            if (token.tokenKind != TokenKind.RBRACE) {
                expressions.add(init());
                while (token.tokenKind == TokenKind.COMMA) {
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
    public SysYIdentifier ident()throws SysYException {
       Token ident = token;
       if (token.tokenKind == TokenKind.IDENT) {
           nextToken();
       } else {
           throw new SysYException(EKind.o, token.line);
       }
       return new SysYIdentifier(ident);
    }

    /**
     * FuncType → 'void' | 'int'
     * @return true for return int value
     */
    public boolean funcType() throws SysYException {
        Token type = token;
        if (isFuncType()) {
            nextToken();
            printTree("<FuncType>");
        } else {
            throw new SysYException(EKind.o, token.line);
        }
        return type.tokenKind == TokenKind.INT;
    }

    /**
     * Begin parse function definition with tokenKind == INT/VOID.
     */
    public SysYFuncDef funcDef() throws SysYException {
        boolean returnInt;
        SysYIdentifier ident;
        List<SysYSymbol> funcParams;
        SysYStatement block;
        SysYFuncDef def;

        returnInt = funcType();
        ident = ident();
        accept(TokenKind.LPAR);

        if (token.tokenKind != TokenKind.RPAR) funcParams = funcFParams();
        else funcParams = new ArrayList<>();
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
        SysYSymbol funcParam = funcFParam();
        if (funcParam == null) return funcParams;
        funcParams.add(funcParam);
        while (token.tokenKind == TokenKind.COMMA) {
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
        if (token.tokenKind == TokenKind.INT) {
            nextToken();
        } else {
            return null;
        }
        SysYIdentifier ident = ident();

        if (token.tokenKind == TokenKind.LSQU) {
            nextToken();
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.tokenKind == TokenKind.LSQU) {
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
        switch (token.tokenKind) {
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
                if (token.tokenKind == TokenKind.ELSE) {
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
                int line = token.line;

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
                int line = token.line;

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
                int line = token.line;

                nextToken();
                SysYExpression result = token.tokenKind == TokenKind.SEMI ? null : exp();

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
                int line = token.line;

                nextToken();
                accept(TokenKind.LPAR);
                if (token.tokenKind == TokenKind.FORMATS) {
                    String format = token.value;
                    nextToken();
                    List<SysYExpression> expressions = new ArrayList<>();
                    while (token.tokenKind == TokenKind.COMMA) {
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
                int pos = 0;
                boolean flag = false;
                Token temp;
                while ((temp = lookAhead(pos)).tokenKind != TokenKind.SEMI) {
                    pos++;
                    if (temp.tokenKind == TokenKind.ASSIGN) {
                        flag = true;
                        break;
                    }
                }

                if (flag) {
                    SysYLVal lVal = (SysYLVal) lVal();
                    accept(TokenKind.ASSIGN);
                    if (token.tokenKind == TokenKind.GETINT) {
                        statement = new SysYAssign(lVal, new SysYGetInt());
                        accept(TokenKind.GETINT);
                        accept(TokenKind.LPAR);
                        accept(TokenKind.RPAR);
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

        while (token.tokenKind != TokenKind.RBRACE) {
            if (token.tokenKind == TokenKind.CONST) {
                statements.add(constDecl());
            } else if (token.tokenKind == TokenKind.INT) {
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
        if (expression == null) return null;
        printTree("<Exp>");
        return expression;
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     */
    public SysYExpression lVal() throws SysYException {
        SysYIdentifier ident;
        try {
            ident = ident();
        } catch (SysYException e) {
            return null;
        }
        SysYExpression exp;

        if (token.tokenKind == TokenKind.LSQU) {
            nextToken();
            SysYExpression firstExp = exp();
            try {
                accept(TokenKind.RSQU);
            } catch (SysYException e) {
                errors.add(e);
            }
            if (token.tokenKind == TokenKind.LSQU) {
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
        int value = Integer.parseInt(token.value);
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
        switch (token.tokenKind) {
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
                if (expression == null) return null;
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
        SysYExpression exp = exp();
        if (exp == null) return funcParams;
        funcParams.add(exp);
        while (token.tokenKind == TokenKind.COMMA) {
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
        nextToken();
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
        } else if (token.tokenKind == TokenKind.IDENT && lookAhead(0).tokenKind == TokenKind.LPAR) {
            SysYIdentifier ident = ident();

            accept(TokenKind.LPAR);
            List<SysYExpression> funcRParams;
            if (token.tokenKind != TokenKind.RPAR) {
                funcRParams = funcRParams();
//                SysYExpression exp = exp();
//                if (exp != null) funcRParams.add(exp);
//                while (token.tokenKind == TokenKind.COMMA) {
//                    nextToken();
//                    funcRParams.add(exp());
//                }
//                printTree("<FuncRParams>");
            } else funcRParams = new ArrayList<>();

            try {
                accept(TokenKind.RPAR);
            } catch (SysYException e) {
                errors.add(e);
            }
            res = new SysYFuncCall(ident, funcRParams);
        } else {
            res = primaryExp();
            if (res == null) return null;
        }
        printTree("<UnaryExp>");
        return res;
    }

    /**
     * MulExp → UnaryExp {('*' | '/' | '%') UnaryExp}
     */
    public SysYExpression mulExp() throws SysYException {
        SysYExpression temp = unaryExp();
        if (temp == null) return null;
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
        if (temp == null) return null;
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
        if (temp == null) return null;
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
        if (temp == null) return null;
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
        if (temp == null) return null;
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
        if (temp == null) return null;
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
