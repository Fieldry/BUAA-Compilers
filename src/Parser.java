import token.Tokens.*;
import tree.SysYTree.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static boolean debug = false;

    private Scanner scanner;
    private Writer writer;
    private boolean ifPrint;

    private Token token;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
        ifPrint = false;
    }

    public Parser(Scanner scanner, Writer writer, boolean printTree) {
        this.scanner = scanner;
        this.writer = writer;
        this.ifPrint = printTree;
    }

    public SysYCompilationUnit syntaxAnalyse() {
        nextToken();
        return parseCompilationUnit();
    }

    private void nextToken() {
        if (token != null) {
            if (debug) System.out.println(token);
            else writer.writeToken(token);
        }
        scanner.nextToken();
        token = scanner.getToken();
    }
    
    private Token lookAhead(int pos) { return scanner.lookAheadToken(pos); }

    private void printTree(String out) {
        if (ifPrint) {
            if (debug) System.out.println(out);
            else writer.write(out);
        }
    }

    /** If next input token matches given token, skip it, otherwise report
     *  an error.
     */
    public void accept(TokenKind tk) {
        if (token.tokenKind == tk) {
            nextToken();
        } else {
            System.exit(1);
        }
    }

    public boolean isFuncType() {
        return token.tokenKind == TokenKind.INT || token.tokenKind == TokenKind.VOID;
    }

    public SysYCompilationUnit parseCompilationUnit() {
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
                    // func def with token == INT
                    top.addFuncDef(funcDef());
                } else if (lookAhead(0).tokenKind == TokenKind.IDENT) {
                    // var declaration with token == INT
                    top.addDecl(decl());
                } else {
                    
                }
            } else {
                
            }
        }
        printTree("<CompUnit>");
        return top;
    }

    /**
     * Begin parse const declaration with tokenKind == "const".
     */
    public SysYDecl constDecl() {
        List<SysYDef> defs = new ArrayList<>();
        accept(TokenKind.CONST);
        accept(TokenKind.INT);
        defs.add(constDef());
        while (token.tokenKind == TokenKind.COMMA) {
            nextToken();
            defs.add(constDef());
        }
        accept(TokenKind.SEMI);
        printTree("<ConstDecl>");
        return new SysYDecl(true, defs);
    }

    /**
     * Begin parse const definition with tokenKind == identifier.
     */
    public SysYDef constDef() {
        Token ident = token;
        int dimension = 0;
        SysYExpression exp = null;
        SysYExpression exp2 = null;
        SysYExpression init = null;
        accept(TokenKind.IDENT);
        if (token.tokenKind == TokenKind.LSQU) {
            nextToken();
            exp = constExp();
            dimension = 1;
            accept(TokenKind.RSQU);
            if (token.tokenKind == TokenKind.LSQU) {
                nextToken();
                exp2 = constExp();
                dimension = 2;
                accept(TokenKind.RSQU);
            }
        }

        if (token.tokenKind == TokenKind.ASSIGN) {
            nextToken();
            init = constInit();
        }

        printTree("<ConstDef>");
        return new SysYDef(true, new SysYIdentifier(ident), dimension, exp, exp2, init);
    }

    /**
     * Begin parse const initializers with token after '='.
     */
    public SysYExpression constInit() {
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
    public SysYDecl decl() {
        List<SysYDef> defs = new ArrayList<>();
        accept(TokenKind.INT);
        defs.add(def());
        while (token.tokenKind == TokenKind.COMMA) {
            nextToken();
            defs.add(def());
        }
        accept(TokenKind.SEMI);
        printTree("<VarDecl>");
        return new SysYDecl(false, defs);
    }

    /**
     * Begin parse definition with tokenKind == identifier.
     */
    public SysYDef def() {
        Token ident = token;
        int dimension = 0;
        SysYExpression exp = null;
        SysYExpression exp2 = null;
        SysYExpression init = null;
        accept(TokenKind.IDENT);
        if (token.tokenKind == TokenKind.LSQU) {
            nextToken();
            exp = constExp();
            dimension = 1;
            accept(TokenKind.RSQU);
            if (token.tokenKind == TokenKind.LSQU) {
                nextToken();
                exp2 = constExp();
                dimension = 2;
                accept(TokenKind.RSQU);
            }
        }

        if (token.tokenKind == TokenKind.ASSIGN) {
            nextToken();
            init = init();
        }

        printTree("<VarDef>");
        return new SysYDef(false, new SysYIdentifier(ident), dimension, exp, exp2, init);
    }

    /**
     * Begin parse initializers with token after '='.
     */
    public SysYExpression init() {
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
     * Begin parse function definition with tokenKind == INT/VOID.
     */
    public SysYFuncDef funcDef() {
        SysYExpression type = null;
        SysYExpression ident = null;
        List<SysYFuncParam> funcParams = null;
        SysYStatement block;
        if (isFuncType()) {
            type = new SysYLiteral(token);
            nextToken();
            printTree("<FuncType>");
        } else {

        }
        if (token.tokenKind == TokenKind.IDENT) {
            ident = new SysYIdentifier(token);
            nextToken();
        } else {

        }
        accept(TokenKind.LPAR);
        if (token.tokenKind != TokenKind.RPAR) funcParams = funcFParams();
        accept(TokenKind.RPAR);
        block = block();
        printTree("<FuncDef>");
        return new SysYFuncDef(type, ident, funcParams, block);
    }

    /**
     * Begin parse function fake parameters with tokenKind == INT.
     */
    public List<SysYFuncParam> funcFParams() {
        List<SysYFuncParam> funcParams = new ArrayList<>();
        funcParams.add(funcFParam());
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
    public SysYFuncParam funcFParam() {
        SysYFuncParam param;
        accept(TokenKind.INT);
        if (token.tokenKind == TokenKind.IDENT) {
            SysYExpression ident = new SysYIdentifier(token);
            nextToken();
            if (token.tokenKind == TokenKind.LSQU) {
                nextToken();
                accept(TokenKind.RSQU);
                if (token.tokenKind == TokenKind.LSQU) {
                    nextToken();
                    SysYExpression constExp = constExp();
                    accept(TokenKind.RSQU);
                    param =  new SysYFuncParam(ident, 2, constExp);
                } else {
                    param =  new SysYFuncParam(ident, 1);
                }
            } else {
                param = new SysYFuncParam(ident, 0);
            }
        } else {
            // error
            param = null;
        }
        printTree("<FuncFParam>");
        return param;
    }

    /**
     * Begin parse function definition with tokenKind == MAIN.
     */
    public SysYMainFuncDef mainFuncDef() {
        accept(TokenKind.MAIN);
        accept(TokenKind.LPAR);
        accept(TokenKind.RPAR);
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
    public SysYStatement statement() {
        SysYStatement statement = null;
        switch (token.tokenKind) {
            case LBRACE -> statement = block();
            case IF -> {
                nextToken();
                accept(TokenKind.LPAR);
                SysYExpression cond = cond();
                accept(TokenKind.RPAR);
                SysYStatement thenStmt = statement();
                SysYStatement elseStmt = null;
                if (token.tokenKind == TokenKind.ELSE) {
                    nextToken();
                    elseStmt = statement();
                }
                statement = new SysYIf(cond, thenStmt, elseStmt);
            }
            case WHILE -> {
                nextToken();
                accept(TokenKind.LPAR);
                SysYExpression cond = cond();
                accept(TokenKind.RPAR);
                SysYStatement thenStmt = statement();
                statement = new SysYWhile(cond, thenStmt);
            }
            case CONTINUE -> {
                nextToken();
                accept(TokenKind.SEMI);
                statement = new SysYContinue();
            }
            case BREAK -> {
                nextToken();
                accept(TokenKind.SEMI);
                statement = new SysYBreak();
            }
            case RETURN -> {
                nextToken();
                SysYExpression result = token.tokenKind == TokenKind.SEMI ? null : exp();
                accept(TokenKind.SEMI);
                statement = new SysYReturn(result);
            }
            case SEMI -> {
                nextToken();
                statement = new SysYExpressionStatement();
            }
            case PRINTF -> {
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
                    statement = new SysYPrintf(format, expressions);
                }
                accept(TokenKind.RPAR);
                accept(TokenKind.SEMI);
            }
            default -> {
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
                    SysYExpression lVal = lVal();
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

                accept(TokenKind.SEMI);
            }
        }
        printTree("<Stmt>");
        return new SysYStmt(statement);
    }

    /**
     * Begin parse block with token == '{'.
     */
    public SysYStatement block() {
        List<SysYStatement> statements = new ArrayList<>();
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
        accept(TokenKind.RBRACE);
        printTree("<Block>");
        return new SysYBlock(statements);
    }

    /*--------parse expressions--------*/
    public SysYExpression exp() {
        SysYExpression expression = addExp();
        printTree("<Exp>");
        return new SysYExp(expression);
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

    /**
     * Parenthesized Expression → '(' Exp ')'
     */
    public SysYExpression parentExp() {
        accept(TokenKind.LPAR);
        SysYExpression exp = exp();
        accept(TokenKind.RPAR);
        return new SysYParens(exp);
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     */
    public SysYExpression lVal() {
        SysYExpression ident = new SysYIdentifier(token);
        SysYExpression exp;
        accept(TokenKind.IDENT);
        if (token.tokenKind == TokenKind.LSQU) {
            nextToken();
            SysYExpression firstExp = exp();
            accept(TokenKind.RSQU);
            if (token.tokenKind == TokenKind.LSQU) {
                nextToken();
                SysYExpression secondExp = exp();
                accept(TokenKind.RSQU);
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
    public SysYExpression primaryExp() {
        SysYExpression expression;
        switch (token.tokenKind) {
            case LPAR -> expression = new SysYPrimaryExp(1, parentExp());
            case INTC -> expression = number();
            default -> expression = new SysYPrimaryExp(2, lVal());
        }
        printTree("<PrimaryExp>");
        return expression;
    }

    /**
     * FuncRParams → Exp { ',' Exp }
     */
    public List<SysYExpression> funcRParams() {
        List<SysYExpression> funcParams = new ArrayList<>();
        funcParams.add(exp());
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
    public SysYExpression unaryExp() {
        SysYExpression res;
        if (isUnaryOp()) {
            Token op = unaryOp();
            SysYExpression exp = unaryExp();
            res = new SysYUnaryExp(op, exp);
        } else if (token.tokenKind == TokenKind.IDENT && lookAhead(0).tokenKind == TokenKind.LPAR) {
            Token ident = token;
            nextToken();
            accept(TokenKind.LPAR);
            List<SysYExpression> funcParams = null;
            if (token.tokenKind != TokenKind.RPAR) {
                funcParams = funcRParams();
            }
            accept(TokenKind.RPAR);
            res = new SysYUnaryExp(new SysYIdentifier(ident), funcParams);
        } else {
            res = new SysYUnaryExp(primaryExp());
        }
        printTree("<UnaryExp>");
        return res;
    }

    /**
     * MulExp → UnaryExp {('*' | '/' | '%') UnaryExp}
     */
    public SysYExpression mulExp() {
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
    public SysYExpression addExp() {
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
    public SysYExpression relExp() {
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
    public SysYExpression eqExp() {
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
    public SysYExpression lAndExp() {
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
    public SysYExpression lOrExp() {
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
    public SysYExpression cond() {
        SysYExpression cond = lOrExp();
        printTree("<Cond>");
        return new SysYCond(lOrExp());
    }

    public SysYExpression constExp() {
        SysYExpression expression = addExp();
        printTree("<ConstExp>");
        return new SysYExp(expression);
    }
}
