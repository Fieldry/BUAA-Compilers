package tree;

import token.Tokens.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Root class for abstract syntax tree nodes. It provides definitions
 * for specific tree nodes as subclasses nested inside.
 */

public class SysYTree {
    public static class SysYCompilationUnit extends SysYTree {
        List<SysYDecl> decls;
        List<SysYFuncDef> funcDefs;
        SysYMainFuncDef mainFuncDef;

        public SysYCompilationUnit() {
            decls = new ArrayList<>();
            funcDefs = new ArrayList<>();
            mainFuncDef = null;
        }

        public SysYCompilationUnit(List<SysYDecl> decls,
                                   List<SysYFuncDef> funcDefs, SysYMainFuncDef mainFuncDef) {
            this.decls = decls;
            this.funcDefs = funcDefs;
            this.mainFuncDef = mainFuncDef;
        }

        public void addDecl(SysYDecl decl) { decls.add(decl); }
        public void addFuncDef(SysYFuncDef funcDef) { funcDefs.add(funcDef); }
        public void setMainFuncDef(SysYMainFuncDef mainFuncDef) { this.mainFuncDef = mainFuncDef; }
    }

    /** Base class for statements. */
    public static class SysYStatement extends SysYTree {}

    /** Var declaration including const var and var. */
    public static class SysYDecl extends SysYStatement {
        public boolean isConst;
        List<SysYDef> defs;

        public SysYDecl(boolean isConst, List<SysYDef> defs) {
            this.isConst = isConst;
            this.defs = defs;
        }
    }

    public static class SysYDef {
        public boolean isConst;
        public SysYExpression ident;
        public int dimensions;
        public SysYExpression firstExp;
        public SysYExpression secondExp;
        public SysYExpression init;

        public SysYDef(boolean isConst, SysYExpression ident, int dimensions,
                        SysYExpression firstExp, SysYExpression secondExp, SysYExpression init) {
            this.isConst = isConst;
            this.ident = ident;
            this.dimensions = dimensions;
            this.firstExp = firstExp;
            this.secondExp = secondExp;
            this.init = init;
        }
    }

    public static class SysYInit extends SysYExpression {
        public boolean isConst;
        public List<SysYExpression> expression;

        public SysYInit(boolean isConst, List<SysYExpression> expression) {
            this.isConst = isConst;
            this.expression = expression;
        }
    }

    /** Function definitions including void and int. */
    public static class SysYFuncDef {
        public SysYExpression funcType;
        public SysYExpression ident;
        public List<? extends SysYExpression> funcParams;
        public SysYStatement block;

        public SysYFuncDef(SysYExpression funcType, SysYExpression ident,
                           List<? extends SysYExpression> funcParams, SysYStatement block) {
            this.funcType = funcType;
            this.ident = ident;
            this.funcParams = funcParams;
            this.block = block;
        }
    }

    public static class SysYFuncParam extends SysYExpression {
        public SysYExpression ident;
        public int dimensions;
        public SysYExpression secondExp;

        public SysYFuncParam(SysYExpression ident, int dimensions) {
            this.ident = ident;
            this.dimensions = dimensions;
        }

        public SysYFuncParam(SysYExpression ident, int dimensions, SysYExpression secondExp) {
            this.ident = ident;
            this.dimensions = dimensions;
            this.secondExp = secondExp;
        }
    }

    public static class SysYMainFuncDef {
        SysYStatement block;

        public SysYMainFuncDef(SysYStatement block) {
            this.block = block;
        }
    }

    public static class SysYStmt extends SysYStatement {
        public SysYStatement statement;

        public SysYStmt(SysYStatement statement) {
            this.statement = statement;
        }
    }

    public static class SysYAssign extends SysYStatement {
        public SysYExpression lVal;
        public SysYExpression expression;
        public SysYStatement getint;

        public SysYAssign(SysYExpression lVal, SysYStatement getint) {
            this.lVal = lVal;
            this.getint = getint;
        }

        public SysYAssign(SysYExpression lVal, SysYExpression expression) {
            this.lVal = lVal;
            this.expression = expression;
        }

        public SysYExpression getLVal() {
            return lVal;
        }

        public SysYExpression getExpression() {
            return expression;
        }
    }

    public static class SysYBlock extends SysYStatement {
        public List<SysYStatement> block;

        public SysYBlock() { this.block = new ArrayList<>(); }

        public SysYBlock(List<SysYStatement> block) { this.block = block; }

        public List<SysYStatement> getBlock() { return block; }
    }

    public static class SysYIf extends SysYStatement {
        public SysYExpression cond;
        public SysYStatement thenStmt;
        public SysYStatement elseStmt;

        public SysYIf(SysYExpression cond, SysYStatement thenStmt, SysYStatement elseStmt) {
            this.cond = cond;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

        public SysYExpression getCondition() {
            return cond;
        }

        public SysYStatement getElseStatement() {
            return elseStmt;
        }

        public SysYStatement getThenStatement() {
            return thenStmt;
        }
    }

    public static class SysYWhile extends SysYStatement {
        public SysYExpression cond;
        public SysYStatement stmt;

        public SysYWhile(SysYExpression cond, SysYStatement stmt) {
            this.cond = cond;
            this.stmt = stmt;
        }

        public SysYExpression getCondition() {
            return cond;
        }

        public SysYStatement getStatement() {
            return stmt;
        }
    }

    public static class SysYExpressionStatement extends SysYStatement {
        public SysYExpression exp;
        public boolean isEmpty;

        public SysYExpressionStatement() { isEmpty = true; }
        public SysYExpressionStatement(SysYExpression exp) {
            this.exp = exp;
            isEmpty = false;
        }

        public SysYExpression getExpression() {
            return exp;
        }
        public boolean isEmpty() {
            return isEmpty;
        }
    }

    public static class SysYBreak extends SysYStatement {

    }

    public static class SysYContinue extends SysYStatement {

    }

    public static class SysYReturn extends SysYStatement {
        public SysYExpression expression;

        public SysYReturn(SysYExpression expression) {
            this.expression = expression;
        }

        public SysYExpression getExpression() {
            return expression;
        }
    }

    public static class SysYGetInt extends SysYStatement {

    }

    public static class SysYPrintf extends SysYStatement {
        public String format;
        public List<SysYExpression> exps;

        public SysYPrintf(String format, List<SysYExpression> exps) {
            this.format = format;
            this.exps = exps;
        }
    }

    /** Base class for expressions. */
    public static class SysYExpression extends SysYTree {}

    public static class SysYLiteral extends SysYExpression {
        Token token;

        public SysYLiteral(Token token) { this.token = token; }

        public Token getToken() { return token; }
    }

    public static class SysYExp extends SysYExpression {
        public SysYExpression expression;

        public SysYExp(SysYExpression expression) {
            this.expression = expression;
        }

        public SysYExpression getExpression() {
            return expression;
        }
    }

    public static class SysYIdentifier extends SysYExpression {
        public Token name;

        public SysYIdentifier(Token token) {
            name = token;
        }

        public Token getName() {
            return name;
        }
    }

    public static class SysYLVal extends SysYExpression {
        public SysYExpression ident;
        public int dimensions;
        public SysYExpression firstExp;
        public SysYExpression secondExp;

        public SysYLVal(SysYExpression ident, int dimensions) {
            this.ident = ident;
            this.dimensions = dimensions;
        }

        public SysYLVal(SysYExpression ident, int dimensions, SysYExpression firstExp) {
            this.ident = ident;
            this.dimensions = dimensions;
            this.firstExp = firstExp;
        }

        public SysYLVal(SysYExpression ident, int dimensions, SysYExpression firstExp, SysYExpression secondExp) {
            this.ident = ident;
            this.dimensions = dimensions;
            this.firstExp = firstExp;
            this.secondExp = secondExp;
        }

        public SysYExpression getIdent() {
            return ident;
        }

        public int getDimensions() {
            return dimensions;
        }

        public SysYExpression getFirstExp() {
            return firstExp;
        }

        public SysYExpression getSecondExp() {
            return secondExp;
        }
    }

    public static class SysYCond extends SysYExpression {
        public SysYExpression cond;

        public SysYCond(SysYExpression cond) { this.cond = cond; }

        public SysYExpression getExpression() {
            return cond;
        }
    }

    public static class SysYParens extends SysYExpression {
        public SysYExpression exp;

        public SysYParens(SysYExpression exp) {
            this.exp = exp;
        }

        public SysYExpression getExpression() {
            return exp;
        }
    }

    public static class SysYUnaryExp extends SysYExpression {
        public SysYExpression primaryExp;
        public SysYExpression ident;
        public List<SysYExpression> funcRParams;
        public Token unaryOp;
        public SysYExpression unaryExp;

        public SysYUnaryExp(SysYExpression primaryExp) {
            this.primaryExp = primaryExp;
        }

        public SysYUnaryExp(SysYExpression ident, List<SysYExpression> funcParams) {
            this.ident = ident;
            this.funcRParams = funcParams;
        }

        public SysYUnaryExp(Token unaryOp, SysYExpression unaryExp) {
            this.unaryOp = unaryOp;
            this.unaryExp = unaryExp;
        }
    }

    public static class SysYMulExp extends SysYExpression {
        public Token token;
        public SysYExpression leftExp;
        public SysYExpression rightExp;

        public SysYMulExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYMulExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.token = token;
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }

        public SysYExpression getLeftExp() {
            return leftExp;
        }

        public SysYExpression getRightExp() {
            return rightExp;
        }
    }

    public static class SysYAddExp extends SysYExpression {
        public Token token;
        public SysYExpression leftExp;
        public SysYExpression rightExp;

        public SysYAddExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYAddExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.token = token;
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }

        public SysYExpression getLeftExp() {
            return leftExp;
        }

        public SysYExpression getRightExp() {
            return rightExp;
        }
    }

    public static class SysYRelExp extends SysYExpression {
        public Token token;
        public SysYExpression leftExp;
        public SysYExpression rightExp;

        public SysYRelExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYRelExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.token = token;
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }

        public SysYExpression getLeftExp() {
            return leftExp;
        }

        public SysYExpression getRightExp() {
            return rightExp;
        }
    }

    public static class SysYEqExp extends SysYExpression {
        public Token token;
        public SysYExpression leftExp;
        public SysYExpression rightExp;

        public SysYEqExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYEqExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.token = token;
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }

        public SysYExpression getLeftExp() {
            return leftExp;
        }

        public SysYExpression getRightExp() {
            return rightExp;
        }
    }

    public static class SysYLAndExp extends SysYExpression {
        public SysYExpression leftExp;
        public SysYExpression rightExp;

        public SysYLAndExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYLAndExp(SysYExpression leftExp, SysYExpression rightExp) {
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }

        public SysYExpression getLeftExp() {
            return leftExp;
        }

        public SysYExpression getRightExp() {
            return rightExp;
        }
    }

    public static class SysYLOrExp extends SysYExpression {
        public SysYExpression leftExp;
        public SysYExpression rightExp;

        public SysYLOrExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYLOrExp(SysYExpression leftExp, SysYExpression rightExp) {
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }

        public SysYExpression getLeftExp() {
            return leftExp;
        }

        public SysYExpression getRightExp() {
            return rightExp;
        }
    }

    public static class SysYPrimaryExp extends SysYExpression {
        public int mode;
        public SysYExpression parExp;
        public SysYExpression lVal;
        public int number;

        public SysYPrimaryExp(int mode, SysYExpression exp) {
            this.mode = mode;
            if (mode == 1) parExp = exp;
            else lVal = exp;
        }

        public SysYPrimaryExp(int number) {
            this.number = number;
        }
    }

    public static class SysYIntC extends SysYExpression {
        public int value;

        public SysYIntC(int value) {
            this.value = value;
        }
    }
}
