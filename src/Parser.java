import token.Tokens.*;
import tree.SysYTree.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private Scanner scanner;

    private Token token;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    private void nextToken() {
        scanner.nextToken();
        token = scanner.getToken();
    }

    /** If next input token matches given token, skip it, otherwise report
     *  an error.
     */
    public void accept(TokenKind tk) {
        if (token.tokenKind == tk) {
            nextToken();
        } else {

        }
    }

    public boolean isFuncType() {
        return token.tokenKind == TokenKind.INT || token.tokenKind == TokenKind.VOID;
    }
    
    public SysYCompilationUnit parseCompilationUnit() {
        while (token.tokenKind == TokenKind.CONST) {
            SysYDecl constDecl = constDecl();
        }

        while (isFuncType()) {
            if (token.tokenKind == TokenKind.VOID) {
                nextToken();
                SysYFuncDef funcDef = funcDef(new SysYLiteral(token));
            } else {
                nextToken();
                while (token.tokenKind != TokenKind.MAIN) {
                    Token prevToken = token;
                    accept(TokenKind.IDENT);
                    if (token.tokenKind == TokenKind.LPAR) {
                        // func def
                        SysYFuncDef funcDef = funcDef(new SysYLiteral(token),
                                                        new SysYIdentifier(prevToken));
                    } else {
                        // var declaration
                    }
                }
                SysYMainFuncDef mainFuncDef = mainFuncDef();
            }
        }

        return null;
    }

    public SysYDecl constDecl() {

        return null;
    }

    public SysYFuncDef funcDef() {
        SysYExpression type = null;
        if (token.tokenKind == TokenKind.VOID) {
            type = new SysYLiteral(token);
            nextToken();
        }
        else if (token.tokenKind == TokenKind.INT) {
            type = new SysYLiteral(token);
            nextToken();
        }
        else {

        }
        return funcDef(type);
    }

    public SysYFuncDef funcDef(SysYExpression type) {
        SysYExpression ident = null;
        if (token.tokenKind == TokenKind.IDENT) {
            ident = new SysYIdentifier(token);
            nextToken();
        } else {

        }
        return funcDef(type, ident);
    }

    public SysYFuncDef funcDef(SysYExpression type, SysYExpression ident) {
        accept(TokenKind.LPAR);
        List<SysYFuncParam> funcParams = new ArrayList<>();
        SysYFuncParam funcParam;
        while ((funcParam = funcParam()) != null) {
            funcParams.add(funcParam);
        }
        accept(TokenKind.RPAR);
        SysYStatement block = block();
        return new SysYFuncDef(type, ident, funcParams, block);
    }

    public SysYFuncParam funcParam() {
        accept(TokenKind.INT);
        if (token.tokenKind == TokenKind.IDENT) {
            SysYExpression ident = new SysYIdentifier(token);
            nextToken();
            if (token.tokenKind == TokenKind.LSQU) {
                accept(TokenKind.RSQU);
                if (token.tokenKind == TokenKind.LSQU) {
                    SysYExpression constExp = parseExpression();
                    accept(TokenKind.RSQU);
                    return new SysYFuncParam(ident, 2, constExp);
                } else {
                    return new SysYFuncParam(ident, 1);
                }
            } else {
                return new SysYFuncParam(ident, 0);
            }
        } else {

        }
        return null;
    }

    public SysYMainFuncDef mainFuncDef() {
        accept(TokenKind.LPAR);
        accept(TokenKind.RPAR);
        return new SysYMainFuncDef(block());
    }

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
    public SysYStatement parseStatement() {
        switch (token.tokenKind) {
            case LBRACE: {

            }
            case IF: {
                nextToken();
                accept(TokenKind.LPAR);
                SysYExpression cond = parseExpression();
                accept(TokenKind.RPAR);
                SysYStatement thenStmt = parseStatement();
                SysYStatement elseStmt = null;
                if (token.tokenKind == TokenKind.ELSE) {
                    nextToken();
                    elseStmt = parseStatement();
                }
                return new SysYIf(cond, thenStmt, elseStmt);
            }
            case WHILE: {
                nextToken();
                accept(TokenKind.LPAR);
                SysYExpression cond = parseExpression();
                accept(TokenKind.RPAR);
                SysYStatement thenStmt = parseStatement();
                return new SysYWhile(cond, thenStmt);
            }
            case CONTINUE: {
                accept(TokenKind.SEMI);
                return new SysYContinue();
            }
            case BREAK: {
                accept(TokenKind.SEMI);
                return new SysYBreak();
            }
            case RETURN: {
                nextToken();
                SysYExpression result = token.tokenKind == TokenKind.SEMI ? null : parseExpression();
                accept(TokenKind.SEMI);
                return new SysYReturn(result);
            }
            case SEMI: {
                nextToken();
                return new SysYExpressionStatement();
            }
            case PRINTF: {

                return null;
            }
            default:
                return null;
        }
    }

    public SysYStatement block() {
        return null;
    }

    /*--------parse expressions--------*/

    /**
     * Ident -> Identifier
     */
    public Token ident() {
        return null;
    }

    public SysYExpression literal() {
        return null;
    }

    public SysYExpression parseExpression() {
        return null;
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

    public SysYExpression unaryExp() {
        return null;
    }

    /**
     * MulExp → UnaryExp {('*' | '/' | '%') UnaryExp}
     */
    public SysYExpression mulExp() {
        SysYExpression temp = unaryExp();
        SysYExpression res = new SysYMulExp(temp);
        while (isMulOp()) {
            Token prevToken = token;
            nextToken();
            temp = unaryExp();
            res = new SysYMulExp(prevToken, res, temp);
        }
        return res;
    }

    /**
     * AddExp → MulExp {('+' | '−') MulExp}
     */
    public SysYExpression addExp() {
        SysYExpression temp = mulExp();
        SysYExpression res = new SysYAddExp(temp);
        while (isAddOp()) {
            Token prevToken = token;
            nextToken();
            temp = mulExp();
            res = new SysYAddExp(prevToken, res, temp);
        }
        return res;
    }

    /**
     * RelExp → AddExp {('<' | '>' | '<=' | '>=') AddExp}
     */
    public SysYExpression relExp() {
        SysYExpression temp = addExp();
        SysYExpression res = new SysYRelExp(temp);
        while (isRelOp()) {
            Token prevToken = token;
            nextToken();
            temp = addExp();
            res = new SysYRelExp(prevToken, res, temp);
        }
        return res;
    }

    /**
     * EqExp → RelExp {('==' | '!=') RelExp}
     */
    public SysYExpression eqExp() {
        SysYExpression temp = relExp();
        SysYExpression res = new SysYEqExp(temp);
        while (isEqOp()) {
            Token prevToken = token;
            nextToken();
            temp = relExp();
            res = new SysYEqExp(prevToken, res, temp);
        }
        return res;
    }

    /**
     * LAndExp → EqExp {'&&' EqExp}
     */
    public SysYExpression lAndExp() {
        SysYExpression temp = eqExp();
        SysYExpression res = new SysYLAndExp(temp);
        while (isLAndOp()) {
            nextToken();
            temp = eqExp();
            res = new SysYLAndExp(res, temp);
        }
        return res;
    }

    /**
     * LOrExp → LAndExp {'||' LAndExp}
     */
    public SysYExpression lOrExp() {
        SysYExpression temp = lAndExp();
        SysYExpression res = new SysYLOrExp(temp);
        while (isLOrOp()) {
            nextToken();
            temp = lAndExp();
            res = new SysYLOrExp(res, temp);
        }
        return res;
    }

    /**
     * Parenthesized Expression → '(' Exp ')'
     */
    public SysYExpression parentExp() {
        accept(TokenKind.LPAR);
        SysYExpression exp = parseExpression();
        accept(TokenKind.RPAR);
        return new SysYParens(exp);
    }

    /**
     * PrimaryExp → '(' Exp ')'
     * 		| LVal
     * 		| Number
     */
    public SysYExpression primaryExp() {
        switch (token.tokenKind) {
            case LPAR: return new SysYPrimaryExp(1, parentExp());
            case INTC: {
                int value = Integer.parseInt(token.value);
                nextToken();
                return new SysYIntC(value);
            }
            default: return new SysYPrimaryExp(2, lVal());
        }
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     */
    public SysYExpression lVal() {
        SysYExpression ident = new SysYIdentifier(ident());
        if (token.tokenKind == TokenKind.LSQU) {
            SysYExpression firstExp = parseExpression();
            accept(TokenKind.RSQU);
            if (token.tokenKind == TokenKind.LSQU) {
                SysYExpression secondExp = parseExpression();
                accept(TokenKind.RSQU);
                return new SysYLVal(ident, 2, firstExp, secondExp);
            } else {
                return new SysYLVal(ident, 1, firstExp);
            }
        } else {
            return new SysYLVal(ident, 0);
        }
    }
}
