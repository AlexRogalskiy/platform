package platform.server.data.expr;

import platform.server.data.query.*;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.caches.hash.HashContext;

import java.util.Map;

@TranslateExprLazy
public abstract class InnerExpr extends VariableClassExpr implements JoinData {

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    public Expr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    @Override
    public DataWhereSet getFollows() {
        return ((DataWhere)getWhere()).getFollows();
    }

    public abstract class NotNull extends DataWhere {

        public InnerExpr getExpr() {
            return InnerExpr.this;
        }

        public String getSource(CompileSource compile) {
            return InnerExpr.this.getSource(compile) + " IS NOT NULL";
        }

        @Override
        protected String getNotSource(CompileSource compile) {
            return InnerExpr.this.getSource(compile) + " IS NULL";
        }

        public Where translateDirect(DirectTranslator translator) {
            return InnerExpr.this.translateDirect(translator).getWhere();
        }
        public Where translateQuery(QueryTranslator translator) {
            return InnerExpr.this.translateQuery(translator).getWhere();
        }

        public void enumerate(SourceEnumerator enumerator) {
            InnerExpr.this.enumerate(enumerator);
        }

        protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
            InnerExpr.this.fillAndJoinWheres(joins,andWhere);
        }

        public int hashContext(HashContext hashContext) {
            return InnerExpr.this.hashContext(hashContext);
        }

        @Override
        public boolean twins(AbstractSourceJoin o) {
            return InnerExpr.this.equals(((NotNull) o).getExpr());
        }
    }

    public static <K> DataWhereSet getExprFollows(Map<K, BaseExpr> map) {
        return new DataWhereSet(map.values());
    }
}
