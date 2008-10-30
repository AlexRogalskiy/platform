package platformlocal;

import java.sql.SQLException;
import java.util.*;

public class TmcBusinessLogics extends BusinessLogics<TmcBusinessLogics>{

    public TmcBusinessLogics() {
        super();
    }

    public TmcBusinessLogics(int TestType) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(TestType);
    }

    Class article;
    Class articleGroup;

    Class store;

    Class document;
    Class primaryDocument, secondaryDocument;
    Class paramsDocument;
    Class quantityDocument;
    Class incomeDocument;
    Class outcomeDocument;

    Class extIncomeDocument;
    Class extIncomeDetail;

    Class intraDocument;
    Class extOutcomeDocument;
    Class exchangeDocument;

    Class revalDocument;

    void InitClasses() {

        article = new ObjectClass(4, "Товар", objectClass);
        articleGroup = new ObjectClass(5, "Группа товаров", objectClass);

        store = new ObjectClass(6, "Склад", objectClass);

        document = new ObjectClass(7, "Документ", objectClass);
        primaryDocument = new ObjectClass(8, "Первичный документ", document);
        secondaryDocument = new ObjectClass(9, "Непервичный документ", document);
        quantityDocument = new ObjectClass(10, "Товарный документ", document);
        incomeDocument = new ObjectClass(11, "Приходный документ", quantityDocument);
        outcomeDocument = new ObjectClass(12, "Расходный документ", quantityDocument);
        paramsDocument = new ObjectClass(13, "Порожденный документ", document);

        extIncomeDocument = new ObjectClass(14, "Внешний приход", incomeDocument, primaryDocument);
        extIncomeDetail = new ObjectClass(15, "Внешний приход (строки)", objectClass);

        intraDocument = new ObjectClass(16, "Внутреннее перемещение", incomeDocument, outcomeDocument, primaryDocument, paramsDocument);
        extOutcomeDocument = new ObjectClass(17, "Внешний расход", outcomeDocument, secondaryDocument, paramsDocument);
        exchangeDocument = new ObjectClass(18, "Пересорт", incomeDocument, outcomeDocument, secondaryDocument, paramsDocument);

        revalDocument = new ObjectClass(19, "Переоценка", primaryDocument);

    }

    PropertyGroup baseGroup, artclGroup, artgrGroup, storeGroup, quantGroup, incPrmsGroup, incSumsGroup, outPrmsGroup, outPrmsGroupBefore, outPrmsGroupAfter;

    LDP name;
    LDP artGroup;
    LDP primDocDate, secDocDate, docStore;

    LJP artGroupName;
    LJP docStoreName;
    LJP intraStoreName;
    LJP extIncDetailArticleName;

    LDP extIncDetailDocument, extIncDetailArticle, extIncDetailQuantity;
    LGP extIncQuantity;
    LDP intraQuantity, intraStore;
    LDP extOutQuantity;
    LDP exchangeQuantity;
    LGP exchIncQuantity, exchOutQuantity;

    LJP incQuantity, outQuantity;
    LJP quantity, notZeroQuantity;
    LJP incStore;
    LGP incStoreQuantity, outStoreQuantity;
    LJP dltStoreQuantity;

    LJP docDate;

    LDP extIncDetailPriceIn, extIncDetailVATIn;
    LJP extIncDetailCalcSum;
    LJP extIncDetailCalcSumVATIn, extIncDetailCalcSumPay;
    LDP extIncDetailSumVATIn, extIncDetailSumPay;

    LGP extIncDocumentSumVATIn, extIncDocumentSumPay;

    LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
    LJP extIncDetailCalcPriceOut;
    LDP extIncDetailPriceOut;

    LDP isRevalued;
    LDP revalPriceIn, revalVATIn;
    LDP revalAddBefore, revalVATOutBefore, revalLocTaxBefore;
    LDP revalPriceOutBefore;
    LDP revalAddAfter, revalVATOutAfter, revalLocTaxAfter;
    LDP revalPriceOutAfter;

    LJP changesParams;
    LGP maxChangesParamsDate;
    LGP maxChangesParamsDoc;

    LDP paramsPriceIn, paramsVATIn;
    LDP paramsAdd, paramsVATOut, paramsLocTax;
    LDP paramsPriceOut;

    LJP extIncPriceIn, extIncVATIn;
    LJP extIncAdd, extIncVATOut, extIncLocTax;
    LJP extIncPriceOut;

    LJP primDocPriceIn, primDocVATIn;
    LJP primDocAdd, primDocVATOut, primDocLocTax;
    LJP primDocPriceOut;

    LJP storePriceIn, storeVATIn;
    LJP storeAdd, storeVATOut, storeLocTax;
    LJP storePriceOut;

    void InitProperties() {

        // -------------------------- Group Properties --------------------- //

        baseGroup = new PropertyGroup("Атрибуты");
        artclGroup = new PropertyGroup("Товар");
        artgrGroup = new PropertyGroup("Группа товаров");
        storeGroup = new PropertyGroup("Склад");
        quantGroup = new PropertyGroup("Количество");
        incPrmsGroup = new PropertyGroup("Входные параметры");
        incSumsGroup = new PropertyGroup("Входные суммы");
        outPrmsGroup = new PropertyGroup("Выходные параметры");
        outPrmsGroupBefore = new PropertyGroup("До");
        outPrmsGroup.add(outPrmsGroupBefore);
        outPrmsGroupAfter = new PropertyGroup("После");
        outPrmsGroup.add(outPrmsGroupAfter);

        // -------------------------- Data Properties ---------------------- //

        name = AddDProp(baseGroup, "Имя", Class.stringClass, objectClass);

        artGroup = AddDProp(artgrGroup, "Гр. тов.", articleGroup, article);

        docStore = AddDProp(storeGroup, "Склад", store, document);

        intraStore = AddDProp(storeGroup, "Склад назн.", store, intraDocument);

        extIncDetailDocument = AddDProp(null, "Документ", extIncomeDocument, extIncomeDetail);
        extIncDetailArticle = AddDProp(artclGroup, "Товар", article, extIncomeDetail);

        // -------------------------- Relation Properties ------------------ //

        artGroupName = AddJProp(artgrGroup, "Имя гр. тов.", name, 1, artGroup, 1);
        docStoreName = AddJProp(storeGroup, "Имя склада", name, 1, docStore, 1);
        intraStoreName = AddJProp(storeGroup, "Имя склада (назн.)", name, 1, intraStore, 1);

        extIncDetailArticleName = AddJProp(artclGroup, "Имя товара", name, 1, extIncDetailArticle, 1);

        // -------------------------- Движение товара по количествам ---------------------- //

        extIncDetailQuantity = AddDProp(quantGroup, "Кол-во", Class.doubleClass, extIncomeDetail);

//        extIncQuantity = AddDProp(quantGroup, "Кол-во прих.", Class.doubleClass, extIncomeDocument, article);
        extIncQuantity = AddGProp(quantGroup, "Кол-во прих.", extIncDetailQuantity, true, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraQuantity = AddDProp(quantGroup, "Кол-во внутр.", Class.doubleClass, intraDocument, article);

        extOutQuantity = AddDProp(quantGroup, "Кол-во расх.", Class.doubleClass, extOutcomeDocument, article);

        exchangeQuantity = AddDProp(quantGroup, "Кол-во перес.", Class.doubleClass, exchangeDocument, article, article);

        exchIncQuantity = AddGProp("Прих. перес.", exchangeQuantity, true, 1, 3);
        exchOutQuantity = AddGProp("Расх. перес.", exchangeQuantity, true, 1, 2);

        LP docIncQuantity = AddCProp("абст. кол-во", null, Class.doubleClass, incomeDocument, article);
        incQuantity = AddUProp("Кол-во прих.", 2, 2, 1, docIncQuantity, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchIncQuantity, 1, 2);
        LP docOutQuantity = AddCProp("абст. кол-во", null, Class.doubleClass, outcomeDocument, article);
        outQuantity = AddUProp("Кол-во расх.", 2, 2, 1, docOutQuantity, 1, 2, 1, extOutQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchOutQuantity, 1, 2);

        LP docQuantity = AddCProp("абст. кол-во", null, Class.doubleClass, document, article);
        quantity = AddUProp("Кол-во", 2, 2, 1, docQuantity, 1, 2, 1, incQuantity, 1, 2, 1, outQuantity, 1, 2);
        LSFP notZero = AddWSFProp("((prm1)<>0)",Class.integralClass);
        notZeroQuantity = AddJProp("Есть в док.", notZero, 2, quantity, 1, 2);

        incStore = AddUProp("Склад прих.", 2, 1, 1, docStore, 1, 1, intraStore, 1);

        incStoreQuantity = AddGProp("Прих. на скл.", incQuantity, true, incStore, 1, 2);
        outStoreQuantity = AddGProp("Расх. со скл.", outQuantity, true, docStore, 1, 2);

        dltStoreQuantity = AddUProp("Ост. на скл.", 1, 2, 1, incStoreQuantity, 1, 2, -1, outStoreQuantity, 1, 2);
//        OstArtStore = AddUProp("остаток по складу",1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);

        // -------------------------- Входные параметры ---------------------------- //

        primDocDate = AddDProp(baseGroup, "Дата", Class.dateClass, primaryDocument);
        secDocDate = AddDProp(baseGroup, "Дата", Class.dateClass, secondaryDocument);

        docDate = AddUProp("Дата", 2, 1, 1, secDocDate, 1, 1, primDocDate, 1);

        extIncDetailPriceIn = AddDProp(incPrmsGroup, "Цена пост.", Class.doubleClass, extIncomeDetail);
        extIncDetailVATIn = AddDProp(incPrmsGroup, "НДС пост.", Class.doubleClass, extIncomeDetail);

        // -------------------------- Входные суммы ---------------------------- //

        LMFP multiplyDD = AddMFProp(Class.doubleClass, Class.doubleClass);
        extIncDetailCalcSum = AddJProp(incSumsGroup, "Сумма пост.", multiplyDD, 1, extIncDetailQuantity, 1, extIncDetailPriceIn, 1);

        LSFP percent = AddSFProp("((prm1*prm2)/100)", Class.doubleClass, Class.doubleClass);
        LSFP round = AddSFProp("round(prm1)", Class.doubleClass);

        extIncDetailCalcSumVATIn = AddJProp("Сумма НДС (расч.)", round, 1,
                                   AddJProp("Сумма НДС (расч. - неокр.)", percent, 1, extIncDetailCalcSum, 1, extIncDetailVATIn, 1), 1);

        extIncDetailSumVATIn = AddDProp(incSumsGroup, "Сумма НДС", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumVATIn, extIncDetailCalcSumVATIn, true);

        extIncDetailCalcSumPay = AddUProp("Всего с НДС (расч.)", 1, 1, 1, extIncDetailCalcSum, 1, 1, extIncDetailSumVATIn, 1);

        extIncDetailSumPay = AddDProp(incSumsGroup, "Всего с НДС", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumPay, extIncDetailCalcSumPay, true);

        extIncDocumentSumVATIn = AddGProp(incSumsGroup, "Сумма НДС", extIncDetailSumVATIn, true, extIncDetailDocument, 1);
        extIncDocumentSumPay = AddGProp(incSumsGroup, "Всего с НДС", extIncDetailSumPay, true, extIncDetailDocument, 1);

        // -------------------------- Выходные параметры ---------------------------- //

        extIncDetailAdd = AddDProp(outPrmsGroup, "Надбавка", Class.doubleClass, extIncomeDetail);
        extIncDetailVATOut = AddDProp(outPrmsGroup, "НДС прод.", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailVATOut, extIncDetailVATIn, true);
        extIncDetailLocTax = AddDProp(outPrmsGroup, "Местн. нал.", Class.doubleClass, extIncomeDetail);

        LSFP addPercent = AddSFProp("((prm1*(100+prm2))/100)", Class.doubleClass, Class.doubleClass);
        extIncDetailCalcPriceOut = AddJProp("Цена розн. (расч.)", round, 1,
                                   AddJProp("Цена розн. (расч. - неокр.)", addPercent, 1,
                                   AddJProp("Цена с НДС", addPercent, 1,
                                   AddJProp("Цена с надбавкой", addPercent, 1,
                                           extIncDetailPriceIn, 1,
                                           extIncDetailAdd, 1), 1,
                                           extIncDetailVATOut, 1), 1,
                                           extIncDetailLocTax, 1), 1);

        extIncDetailPriceOut = AddDProp(outPrmsGroup, "Цена розн.", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailPriceOut, extIncDetailCalcPriceOut, true);

        // ------------------------- Фиксирующиеся параметры товара ------------------------- //

        paramsPriceIn = AddDProp(incPrmsGroup, "Цена пост.", Class.doubleClass, paramsDocument, article);
        paramsVATIn = AddDProp(incPrmsGroup, "НДС пост.", Class.doubleClass, paramsDocument, article);
        paramsAdd = AddDProp(outPrmsGroup, "Надбавка", Class.doubleClass, paramsDocument, article);
        paramsVATOut = AddDProp(outPrmsGroup, "НДС прод.", Class.doubleClass, paramsDocument, article);
        paramsLocTax = AddDProp(outPrmsGroup, "Местн. нал.", Class.doubleClass, paramsDocument, article);
        paramsPriceOut = AddDProp(outPrmsGroup, "Цена розн.", Class.doubleClass, paramsDocument, article);

        // ------------------------------ Переоценка -------------------------------- //

        isRevalued = AddDProp("Переоц.", Class.bitClass, revalDocument, article);

        revalPriceIn = AddDProp(incPrmsGroup, "Цена пост.", Class.doubleClass, revalDocument, article);
        revalVATIn = AddDProp(incPrmsGroup, "НДС пост.", Class.doubleClass, revalDocument, article);
        revalAddBefore = AddDProp(outPrmsGroupBefore, "Надбавка (до)", Class.doubleClass, revalDocument, article);
        revalVATOutBefore = AddDProp(outPrmsGroupBefore, "НДС прод. (до)", Class.doubleClass, revalDocument, article);
        revalLocTaxBefore = AddDProp(outPrmsGroupBefore, "Местн. нал. (до)", Class.doubleClass, revalDocument, article);
        revalPriceOutBefore = AddDProp(outPrmsGroupBefore, "Цена розн. (до)", Class.doubleClass, revalDocument, article);
        revalAddAfter = AddDProp(outPrmsGroupAfter, "Надбавка (после)", Class.doubleClass, revalDocument, article);
        revalVATOutAfter = AddDProp(outPrmsGroupAfter, "НДС прод. (после)", Class.doubleClass, revalDocument, article);
        revalLocTaxAfter = AddDProp(outPrmsGroupAfter, "Местн. нал. (после)", Class.doubleClass, revalDocument, article);
        revalPriceOutAfter = AddDProp(outPrmsGroupAfter, "Цена розн. (после)", Class.doubleClass, revalDocument, article);

        // -------------------------- Последний документ ---------------------------- //

        changesParams = AddUProp("Изм. парам.", 2, 2, 1, isRevalued, 1, 2, 1, notZeroQuantity, 1, 2);
        LMFP multiplyBD = AddMFProp(Class.bitClass, Class.dateClass);
        LJP changesParamsDate = AddJProp("Дата изм. пар.", multiplyBD, 2, changesParams, 1, 2, primDocDate, 1);
        maxChangesParamsDate = AddGProp(baseGroup, "Посл. дата изм. парам.", changesParamsDate, false, incStore, 1, 2);

        LSFP equalsDD = AddWSFProp("((prm1)=(prm2)) AND ((prm3)=(prm4))", Class.dateClass, Class.dateClass, store, store);
        LJP primDocIsCor = AddJProp("Док. макс.", equalsDD, 3, primDocDate, 1, maxChangesParamsDate, 2, 3, incStore, 1, 2);

        LMFP multiplyBB = AddMFProp(Class.bitClass, Class.bitClass);
        LJP primDocIsLast = AddJProp("Посл.", multiplyBB, 3, primDocIsCor, 1, 2, 3, changesParams, 1, 3);

        LMFP multiplyBPrimDoc = AddMFProp(Class.bitClass, primaryDocument);
        LJP primDocSelfLast = AddJProp("Тов. док. максю", multiplyBPrimDoc, 3, primDocIsLast, 1, 2, 3, 1);
        maxChangesParamsDoc = AddGProp(baseGroup, "Посл. док. изм. парам.", primDocSelfLast, false, 2, 3);

        // ------------------------- Параметры по приходу --------------------------- //

        LP bitExtInc = AddCProp("Бит", true, Class.bitClass, extIncomeDetail);
        LMFP multiplyBDetail = AddMFProp(Class.bitClass, extIncomeDetail);
        LJP propDetail = AddJProp("", multiplyBDetail, 1, bitExtInc, 1, 1);
        LGP maxDetail = AddGProp("", propDetail, false, extIncDetailDocument, 1, extIncDetailArticle, 1);

        extIncPriceIn = AddJProp(incPrmsGroup, "Цена пост. (прих.)", extIncDetailPriceIn, 2, maxDetail, 1, 2);
        extIncVATIn = AddJProp(incPrmsGroup, "НДС пост. (прих.)", extIncDetailVATIn, 2, maxDetail, 1, 2);
        extIncAdd = AddJProp(outPrmsGroup, "Надбавка (прих.)", extIncDetailAdd, 2, maxDetail, 1, 2);
        extIncVATOut = AddJProp(outPrmsGroup, "НДС прод. (прих.)", extIncDetailVATOut, 2, maxDetail, 1, 2);
        extIncLocTax = AddJProp(outPrmsGroup, "Местн. нал. (прих.)", extIncDetailLocTax, 2, maxDetail, 1, 2);
        extIncPriceOut = AddJProp(outPrmsGroup, "Цена розн. (прих.)", extIncDetailPriceOut, 2, maxDetail, 1, 2);

        // ------------------------- Перегруженные параметры ------------------------ //

        LP nullPrimDocArt = AddCProp("null", null, Class.doubleClass, primaryDocument, article);

        primDocPriceIn = AddUProp("Цена пост. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsPriceIn, 1, 2, 1, extIncPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        primDocVATIn = AddUProp("НДС пост. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsVATIn, 1, 2, 1, extIncVATIn, 1, 2, 1, revalVATIn, 1, 2);
        primDocAdd = AddUProp("Надбавка (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsAdd, 1, 2, 1, extIncAdd, 1, 2, 1, revalAddAfter, 1, 2);
        primDocVATOut = AddUProp("НДС прод. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsVATOut, 1, 2, 1, extIncVATOut, 1, 2, 1, revalVATOutAfter, 1, 2);
        primDocLocTax = AddUProp("Местн. нал. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsLocTax, 1, 2, 1, extIncLocTax, 1, 2, 1, revalLocTaxAfter, 1, 2);
        primDocPriceOut = AddUProp("Цена розн. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsPriceOut, 1, 2, 1, extIncPriceOut, 1, 2, 1, revalPriceOutAfter, 1, 2);

        storePriceIn = AddJProp(incPrmsGroup, "Цена пост. (тек.)", primDocPriceIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATIn = AddJProp(incPrmsGroup, "НДС пост. (тек.)", primDocVATIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeAdd = AddJProp(outPrmsGroup, "Надбавка (тек.)", primDocAdd, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATOut = AddJProp(outPrmsGroup, "НДС прод. (тек.)", primDocVATOut, 2, maxChangesParamsDoc, 1, 2, 2);
        storeLocTax = AddJProp(outPrmsGroup, "Местн. нал. (тек.)", primDocLocTax, 2, maxChangesParamsDoc, 1, 2, 2);
        storePriceOut = AddJProp(outPrmsGroup, "Цена розн. (тек.)", primDocPriceOut, 2, maxChangesParamsDoc, 1, 2, 2);

    }

    void InitConstraints() {
    }

    void InitPersistents() {
        Persistents.add((AggregateProperty)incStoreQuantity.Property);
        Persistents.add((AggregateProperty)outStoreQuantity.Property);
        Persistents.add((AggregateProperty)maxChangesParamsDate.Property);
        Persistents.add((AggregateProperty)maxChangesParamsDoc.Property);
    }

    void InitTables() {

        TableImplement Include;

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,store));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,articleGroup));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        Include.add(new DataPropertyInterface(0,document));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        Include.add(new DataPropertyInterface(0,store));
        TableFactory.IncludeIntoGraph(Include);

    }

    void InitIndexes() {
        List<Property> index;

/*        index = new ArrayList();
        index.add(primDocDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(maxChangesParamsDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(docStore.Property);
        Indexes.add(index);*/
    }

    void InitNavigators() {

        createDefaultClassForms(objectClass, baseElement);

        NavigatorForm extIncDetailForm = new ExtIncDetailNavigatorForm(10, "Внешний приход");
        baseElement.addChild(extIncDetailForm);

        NavigatorForm extIncForm = new ExtIncNavigatorForm(15, "Внешний приход по товарам");
        extIncDetailForm.addChild(extIncForm);

        NavigatorForm intraForm = new IntraNavigatorForm(20, "Внутреннее перемещение");
        baseElement.addChild(intraForm);

        NavigatorForm extOutForm = new ExtOutNavigatorForm(30, "Внешний расход");
        baseElement.addChild(extOutForm);

        NavigatorForm exchangeForm = new ExchangeNavigatorForm(40, "Пересорт");
        baseElement.addChild(exchangeForm);

        NavigatorForm revalueForm = new RevalueNavigatorForm(45, "Переоценка");
        baseElement.addChild(revalueForm);

        NavigatorForm storeArticleForm = new StoreArticleNavigatorForm(50, "Товары по складам");
        baseElement.addChild(storeArticleForm);
    }

    private class ExtIncDetailNavigatorForm extends NavigatorForm {

        public ExtIncDetailNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjDetail = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objDetail = new ObjectImplement(IDShift(1), extIncomeDetail, "Строка", gobjDetail);

            addGroup(gobjDoc);
            addGroup(gobjDetail);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, incSumsGroup, objDoc);
            addPropertyView(this, artclGroup, objDetail);
            addPropertyView(this, quantGroup, objDetail);
            addPropertyView(this, incPrmsGroup, objDetail);
            addPropertyView(this, incSumsGroup, objDetail);
            addPropertyView(this, outPrmsGroup, objDetail);

            PropertyObjectImplement detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new Filter(detDocument, FieldExprCompareWhere.EQUALS, new ObjectValueLink(objDoc)));
        }
    }

    private class ExtIncNavigatorForm extends NavigatorForm {

        public ExtIncNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, extIncQuantity, objDoc, objArt);
            addPropertyView(this, incPrmsGroup, objDoc, objArt);
            addPropertyView(this, outPrmsGroup, objDoc, objArt);
        }
    }

    private class IntraNavigatorForm extends NavigatorForm {

        public IntraNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), intraDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, intraQuantity, objDoc, objArt);
            addPropertyView(this, incPrmsGroup, objDoc, objArt);
            addPropertyView(this, outPrmsGroup, objDoc, objArt);
        }
    }

    private class ExtOutNavigatorForm extends NavigatorForm {

        public ExtOutNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extOutcomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, extOutQuantity, objDoc, objArt);
            addPropertyView(this, incPrmsGroup, objDoc, objArt);
            addPropertyView(this, outPrmsGroup, objDoc, objArt);
        }
    }

    private class ExchangeNavigatorForm extends NavigatorForm {

        public ExchangeNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtFrom = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtTo = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), exchangeDocument, "Документ", gobjDoc);
            ObjectImplement objArtFrom = new ObjectImplement(IDShift(1), article, "Товар (с)", gobjArtFrom);
            ObjectImplement objArtTo = new ObjectImplement(IDShift(1), article, "Товар (на)", gobjArtTo);

            addGroup(gobjDoc);
            addGroup(gobjArtFrom);
            addGroup(gobjArtTo);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArtFrom);
            addPropertyView(this, artgrGroup, objArtFrom);
            addPropertyView(this, baseGroup, objArtTo);
            addPropertyView(this, artgrGroup, objArtTo);
            addPropertyView(this, exchIncQuantity, objDoc, objArtFrom);
            addPropertyView(this, exchOutQuantity, objDoc, objArtFrom);
            addPropertyView(this, incPrmsGroup, objDoc, objArtFrom);
            addPropertyView(this, outPrmsGroup, objDoc, objArtFrom);
            addPropertyView(this, exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyView(this, incPrmsGroup, objDoc, objArtTo);
            addPropertyView(this, outPrmsGroup, objDoc, objArtTo);
        }
    }

    private class RevalueNavigatorForm extends NavigatorForm {

        public RevalueNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), revalDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, isRevalued, objDoc, objArt);
            addPropertyView(this, incPrmsGroup, objDoc, objArt);
            addPropertyView(this, outPrmsGroupBefore, objDoc, objArt);
            addPropertyView(this, outPrmsGroupAfter, objDoc, objArt);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {

        public StoreArticleNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjStore = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjStore);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjStore);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objStore);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, objStore, objArt);
        }
    }

    // ------------------------------------- Временные методы --------------------------- //

    void fillData(DataAdapter Adapter) throws SQLException {

        int Modifier = 10;

        Map<Class,Integer> ClassQuantity = new HashMap();
        ClassQuantity.put(article,2*Modifier);
        ClassQuantity.put(articleGroup,((Double)(Modifier*0.3)).intValue());
        ClassQuantity.put(store,((Double)(Modifier*0.3)).intValue());
        ClassQuantity.put(extIncomeDocument,Modifier*2);
        ClassQuantity.put(extIncomeDetail,Modifier*10);
        ClassQuantity.put(intraDocument,Modifier);
        ClassQuantity.put(extOutcomeDocument,Modifier*5);
        ClassQuantity.put(exchangeDocument,Modifier);
        ClassQuantity.put(revalDocument,((Double)(Modifier*0.5)).intValue());

        Map<DataProperty, Set<DataPropertyInterface>> PropNotNulls = new HashMap();
        name.putNotNulls(PropNotNulls,0);
        artGroup.putNotNulls(PropNotNulls,0);
        primDocDate.putNotNulls(PropNotNulls,0);
        secDocDate.putNotNulls(PropNotNulls,0);
        docStore.putNotNulls(PropNotNulls,0);
        intraStore.putNotNulls(PropNotNulls,0);
        extIncDetailDocument.putNotNulls(PropNotNulls,0);
        extIncDetailArticle.putNotNulls(PropNotNulls,0);
        extIncDetailQuantity.putNotNulls(PropNotNulls,0);
        extIncDetailPriceIn.putNotNulls(PropNotNulls,0);
        extIncDetailVATIn.putNotNulls(PropNotNulls,0);

        Map<DataProperty,Integer> PropQuantity = new HashMap();

//        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        PropQuantity.put((DataProperty)intraQuantity.Property,Modifier*2);
        PropQuantity.put((DataProperty)extOutQuantity.Property,Modifier);
        PropQuantity.put((DataProperty)exchangeQuantity.Property,Modifier*2);

        autoFillDB(Adapter,ClassQuantity,PropQuantity,PropNotNulls);
    }

}


