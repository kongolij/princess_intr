package com.promotions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PromotionWrapper {

	@JsonProperty("promotion")
	@NotNull
	private List<Promotion> promotion;

	// Getters and setters

	public static class Promotion {
		@JsonProperty("template")
		private String template;

		@JsonProperty("templateName")
		private String templateName;

		@JsonProperty("templatePath")
		private String templatePath;

		@JsonProperty("displayName")
		private String displayName;

		@JsonProperty("description")
		private String description;

		@JsonProperty("global")
		private boolean global;

		@JsonProperty("enabled")
		private boolean enabled;

		@JsonProperty("type")
		private int type;

		@JsonProperty("priority")
		private int priority;

		@JsonProperty("id")
		@NotNull
		private String id;

		@JsonProperty("startDate")
		private String startDate;

		@JsonProperty("endDate")
		private String endDate;

		@JsonProperty("giveToAnonymousProfiles")
		private boolean giveToAnonymousProfiles;

		@JsonProperty("oneUsePerOrder")
		private boolean oneUsePerOrder;

		@JsonProperty("allowMultiple")
		private boolean allowMultiple;

		@JsonProperty("maxUsesPerAccount")
		private int maxUsesPerAccount;

		@JsonProperty("maxUsesPerUser")
		private int maxUsesPerUser;

		@JsonProperty("maxUsesPerOrder")
		private int maxUsesPerOrder;

		@JsonProperty("uses")
		private int uses;

		@JsonProperty("targetAmount")
		private Integer targetAmount;

		@JsonProperty("targetQuantity")
		private Integer targetQuantity;

		@JsonProperty("shippingMethods")
		private List<String> shippingMethods;

		@JsonProperty("audiences")
		private List<Audience> audiences;

		@JsonProperty("priceListGroups")
		private List<PriceListGroup> priceListGroups;

		@JsonProperty("translations")
		private Translations translations;

		@JsonProperty("templateValues")
		private TemplateValues templateValues;

		@JsonProperty("condition_psc_value")
		private PSCValue conditionPscValue;

		@JsonProperty("optional_offer_psc_value")
		private PSCValue optionalOfferPscValue;

		@JsonProperty("excludedPromotions")
		private List<ExcludedPromotion> excludedPromotions;

		@JsonProperty("parentFolder")
		private ParentFolder parentFolder;

		@JsonProperty("stackingRule")
		private StackingRule stackingRule;

		@JsonProperty("filterForQualifierZeroPrices")
		private Object filterForQualifierZeroPrices;

		@JsonProperty("filterForQualifierNegativePrices")
		private Object filterForQualifierNegativePrices;

		@JsonProperty("filterForQualifierDiscountedByAny")
		private Object filterForQualifierDiscountedByAny;

		@JsonProperty("filterForQualifierActedAsQualifier")
		private Object filterForQualifierActedAsQualifier;

		@JsonProperty("filterForQualifierOnSale")
		private Object filterForQualifierOnSale;

		@JsonProperty("filterForQualifierDiscountedByCurrent")
		private Object filterForQualifierDiscountedByCurrent;

		@JsonProperty("sites")
		private List<Object> sites;

		@JsonProperty("cardIINRanges")
		private List<Object> cardIINRanges;

		@JsonProperty("filterForTargetNegativePrices")
		private Object filterForTargetNegativePrices;

		@JsonProperty("closenessQualifiers")
		private List<ClosenessQualifiers> closenessQualifiers;

		@JsonProperty("filterForTargetDiscountedByCurrent")
		private Object filterForTargetDiscountedByCurrent;

		@JsonProperty("filterForTargetDiscountedByAny")
		private Object filterForTargetDiscountedByAny;

		@JsonProperty("filterForTargetActedAsQualifier")
		private Object filterForTargetActedAsQualifier;

		@JsonProperty("filterForTargetOnSale")
		private Object filterForTargetOnSale;

		@JsonProperty("filterForTargetZeroPrices")
		private Object filterForTargetZeroPrices;

		@JsonProperty("unqualifiedMessages")
		private List<Object> unqualifiedMessages;

		@JsonProperty("qualifiedMessages")
		private List<Object> qualifiedMessages;

		@JsonProperty("filterForTargetPriceLTOETPromoPrice")
		private Object filterForTargetPriceLTOETPromoPrice;

		public String getTemplate() {
			return template;
		}

		public void setTemplate(String template) {
			this.template = template;
		}

		public String getTemplateName() {
			return templateName;
		}

		public void setTemplateName(String templateName) {
			this.templateName = templateName;
		}

		public String getTemplatePath() {
			return templatePath;
		}

		public void setTemplatePath(String templatePath) {
			this.templatePath = templatePath;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public boolean isGlobal() {
			return global;
		}

		public void setGlobal(boolean global) {
			this.global = global;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}

		public boolean isGiveToAnonymousProfiles() {
			return giveToAnonymousProfiles;
		}

		public void setGiveToAnonymousProfiles(boolean giveToAnonymousProfiles) {
			this.giveToAnonymousProfiles = giveToAnonymousProfiles;
		}

		public boolean isOneUsePerOrder() {
			return oneUsePerOrder;
		}

		public void setOneUsePerOrder(boolean oneUsePerOrder) {
			this.oneUsePerOrder = oneUsePerOrder;
		}

		public boolean isAllowMultiple() {
			return allowMultiple;
		}

		public void setAllowMultiple(boolean allowMultiple) {
			this.allowMultiple = allowMultiple;
		}

		public int getMaxUsesPerAccount() {
			return maxUsesPerAccount;
		}

		public void setMaxUsesPerAccount(int maxUsesPerAccount) {
			this.maxUsesPerAccount = maxUsesPerAccount;
		}

		public int getMaxUsesPerUser() {
			return maxUsesPerUser;
		}

		public void setMaxUsesPerUser(int maxUsesPerUser) {
			this.maxUsesPerUser = maxUsesPerUser;
		}

		public int getMaxUsesPerOrder() {
			return maxUsesPerOrder;
		}

		public void setMaxUsesPerOrder(int maxUsesPerOrder) {
			this.maxUsesPerOrder = maxUsesPerOrder;
		}

		public int getUses() {
			return uses;
		}

		public void setUses(int uses) {
			this.uses = uses;
		}

		public Integer getTargetAmount() {
			return targetAmount;
		}

		public void setTargetAmount(Integer targetAmount) {
			this.targetAmount = targetAmount;
		}

		public Integer getTargetQuantity() {
			return targetQuantity;
		}

		public void setTargetQuantity(Integer targetQuantity) {
			this.targetQuantity = targetQuantity;
		}

		public List<String> getShippingMethods() {
			return shippingMethods;
		}

		public void setShippingMethods(List<String> shippingMethods) {
			this.shippingMethods = shippingMethods;
		}

		public List<Audience> getAudiences() {
			return audiences;
		}

		public void setAudiences(List<Audience> audiences) {
			this.audiences = audiences;
		}

		public List<PriceListGroup> getPriceListGroups() {
			return priceListGroups;
		}

		public void setPriceListGroups(List<PriceListGroup> priceListGroups) {
			this.priceListGroups = priceListGroups;
		}

		public Translations getTranslations() {
			return translations;
		}

		public void setTranslations(Translations translations) {
			this.translations = translations;
		}

		public TemplateValues getTemplateValues() {
			return templateValues;
		}

		public void setTemplateValues(TemplateValues templateValues) {
			this.templateValues = templateValues;
		}

		public PSCValue getConditionPscValue() {
			return conditionPscValue;
		}

		public void setConditionPscValue(PSCValue conditionPscValue) {
			this.conditionPscValue = conditionPscValue;
		}

		public PSCValue getOptionalOfferPscValue() {
			return optionalOfferPscValue;
		}

		public void setOptionalOfferPscValue(PSCValue optionalOfferPscValue) {
			this.optionalOfferPscValue = optionalOfferPscValue;
		}

		public List<ExcludedPromotion> getExcludedPromotions() {
			return excludedPromotions;
		}

		public void setExcludedPromotions(List<ExcludedPromotion> excludedPromotions) {
			this.excludedPromotions = excludedPromotions;
		}

		public ParentFolder getParentFolder() {
			return parentFolder;
		}

		public void setParentFolder(ParentFolder parentFolder) {
			this.parentFolder = parentFolder;
		}

		public StackingRule getStackingRule() {
			return stackingRule;
		}

		public void setStackingRule(StackingRule stackingRule) {
			this.stackingRule = stackingRule;
		}

		public Object getFilterForQualifierZeroPrices() {
			return filterForQualifierZeroPrices;
		}

		public void setFilterForQualifierZeroPrices(Object filterForQualifierZeroPrices) {
			this.filterForQualifierZeroPrices = filterForQualifierZeroPrices;
		}

		public Object getFilterForQualifierNegativePrices() {
			return filterForQualifierNegativePrices;
		}

		public void setFilterForQualifierNegativePrices(Object filterForQualifierNegativePrices) {
			this.filterForQualifierNegativePrices = filterForQualifierNegativePrices;
		}

		public Object getFilterForQualifierDiscountedByAny() {
			return filterForQualifierDiscountedByAny;
		}

		public void setFilterForQualifierDiscountedByAny(Object filterForQualifierDiscountedByAny) {
			this.filterForQualifierDiscountedByAny = filterForQualifierDiscountedByAny;
		}

		public Object getFilterForQualifierActedAsQualifier() {
			return filterForQualifierActedAsQualifier;
		}

		public void setFilterForQualifierActedAsQualifier(Object filterForQualifierActedAsQualifier) {
			this.filterForQualifierActedAsQualifier = filterForQualifierActedAsQualifier;
		}

		public Object getFilterForQualifierOnSale() {
			return filterForQualifierOnSale;
		}

		public void setFilterForQualifierOnSale(Object filterForQualifierOnSale) {
			this.filterForQualifierOnSale = filterForQualifierOnSale;
		}

		public Object getFilterForQualifierDiscountedByCurrent() {
			return filterForQualifierDiscountedByCurrent;
		}

		public void setFilterForQualifierDiscountedByCurrent(Object filterForQualifierDiscountedByCurrent) {
			this.filterForQualifierDiscountedByCurrent = filterForQualifierDiscountedByCurrent;
		}

		public List<Object> getSites() {
			return sites;
		}

		public void setSites(List<Object> sites) {
			this.sites = sites;
		}

		public List<Object> getCardIINRanges() {
			return cardIINRanges;
		}

		public void setCardIINRanges(List<Object> cardIINRanges) {
			this.cardIINRanges = cardIINRanges;
		}

		public Object getFilterForTargetNegativePrices() {
			return filterForTargetNegativePrices;
		}

		public void setFilterForTargetNegativePrices(Object filterForTargetNegativePrices) {
			this.filterForTargetNegativePrices = filterForTargetNegativePrices;
		}

		public List<ClosenessQualifiers> getClosenessQualifiers() {
			return closenessQualifiers;
		}

		public void setClosenessQualifiers(List<ClosenessQualifiers> closenessQualifiers) {
			this.closenessQualifiers = closenessQualifiers;
		}

		public Object getFilterForTargetDiscountedByCurrent() {
			return filterForTargetDiscountedByCurrent;
		}

		public void setFilterForTargetDiscountedByCurrent(Object filterForTargetDiscountedByCurrent) {
			this.filterForTargetDiscountedByCurrent = filterForTargetDiscountedByCurrent;
		}

		public Object getFilterForTargetDiscountedByAny() {
			return filterForTargetDiscountedByAny;
		}

		public void setFilterForTargetDiscountedByAny(Object filterForTargetDiscountedByAny) {
			this.filterForTargetDiscountedByAny = filterForTargetDiscountedByAny;
		}

		public Object getFilterForTargetActedAsQualifier() {
			return filterForTargetActedAsQualifier;
		}

		public void setFilterForTargetActedAsQualifier(Object filterForTargetActedAsQualifier) {
			this.filterForTargetActedAsQualifier = filterForTargetActedAsQualifier;
		}

		public Object getFilterForTargetOnSale() {
			return filterForTargetOnSale;
		}

		public void setFilterForTargetOnSale(Object filterForTargetOnSale) {
			this.filterForTargetOnSale = filterForTargetOnSale;
		}

		public Object getFilterForTargetZeroPrices() {
			return filterForTargetZeroPrices;
		}

		public void setFilterForTargetZeroPrices(Object filterForTargetZeroPrices) {
			this.filterForTargetZeroPrices = filterForTargetZeroPrices;
		}

		public List<Object> getUnqualifiedMessages() {
			return unqualifiedMessages;
		}

		public void setUnqualifiedMessages(List<Object> unqualifiedMessages) {
			this.unqualifiedMessages = unqualifiedMessages;
		}

		public List<Object> getQualifiedMessages() {
			return qualifiedMessages;
		}

		public void setQualifiedMessages(List<Object> qualifiedMessages) {
			this.qualifiedMessages = qualifiedMessages;
		}

	}

	public static class TemplateValues {
		@JsonProperty("discount_value")
		private String discountValue;

		@JsonProperty("discount_type_value")
		private String discountTypeValue;

		@JsonProperty("spend_value")
		private String spendValue;

		@JsonProperty("no_of_items_to_discount")
		private String noOfItemsToDiscount;

		@JsonProperty("no_of_items_to_buy")
		private String noOfItemsToBuy;

		@JsonProperty("sort_by")
		private String sortBy;

		@JsonProperty("sort_order")
		private String sortOrder;

		@JsonProperty("PSC_value")
		private PSCValue pscValue;

		@JsonProperty("condition_psc_value")
		private PSCValue conditionPscValue;

		@JsonProperty("optional_offer_psc_value")
		private PSCValue optionalOfferPscValue;

		@JsonProperty("discountStructure")
		private DiscountStructure discountStructure;

		public String getDiscountValue() {
			return discountValue;
		}

		public void setDiscountValue(String discountValue) {
			this.discountValue = discountValue;
		}

		public String getDiscountTypeValue() {
			return discountTypeValue;
		}

		public void setDiscountTypeValue(String discountTypeValue) {
			this.discountTypeValue = discountTypeValue;
		}

		public String getSpendValue() {
			return spendValue;
		}

		public void setSpendValue(String spendValue) {
			this.spendValue = spendValue;
		}

		public String getNoOfItemsToDiscount() {
			return noOfItemsToDiscount;
		}

		public void setNoOfItemsToDiscount(String noOfItemsToDiscount) {
			this.noOfItemsToDiscount = noOfItemsToDiscount;
		}

		public String getNoOfItemsToBuy() {
			return noOfItemsToBuy;
		}

		public void setNoOfItemsToBuy(String noOfItemsToBuy) {
			this.noOfItemsToBuy = noOfItemsToBuy;
		}

		public String getSortBy() {
			return sortBy;
		}

		public void setSortBy(String sortBy) {
			this.sortBy = sortBy;
		}

		public String getSortOrder() {
			return sortOrder;
		}

		public void setSortOrder(String sortOrder) {
			this.sortOrder = sortOrder;
		}

		public PSCValue getPscValue() {
			return pscValue;
		}

		public void setPscValue(PSCValue pscValue) {
			this.pscValue = pscValue;
		}

		public PSCValue getConditionPscValue() {
			return conditionPscValue;
		}

		public void setConditionPscValue(PSCValue conditionPscValue) {
			this.conditionPscValue = conditionPscValue;
		}

		public PSCValue getOptionalOfferPscValue() {
			return optionalOfferPscValue;
		}

		public void setOptionalOfferPscValue(PSCValue optionalOfferPscValue) {
			this.optionalOfferPscValue = optionalOfferPscValue;
		}

		public DiscountStructure getDiscountStructure() {
			return discountStructure;
		}

		public void setDiscountStructure(DiscountStructure discountStructure) {
			this.discountStructure = discountStructure;
		}

		// Getters and setters

	}

	public static class PSCValue {
		@JsonProperty("includedRuleSet")
		private RuleSet includedRuleSet;

		@JsonProperty("includedSkus")
		private List<String> includedSkus;

		@JsonProperty("includedCategories")
		private List<String> includedCategories;

		@JsonProperty("includedProducts")
		private List<String> includedProducts;

		@JsonProperty("excludedSkus")
		private List<String> excludedSkus;

		@JsonProperty("excludedRuleSet")
		private RuleSet excludedRuleSet;

		@JsonProperty("excludedProducts")
		private List<String> excludedProducts;

		@JsonProperty("excludedCategories")
		private List<String> excludedCategories;

		public RuleSet getIncludedRuleSet() {
			return includedRuleSet;
		}

		public void setIncludedRuleSet(RuleSet includedRuleSet) {
			this.includedRuleSet = includedRuleSet;
		}

		public List<String> getIncludedSkus() {
			return includedSkus;
		}

		public void setIncludedSkus(List<String> includedSkus) {
			this.includedSkus = includedSkus;
		}

		public List<String> getIncludedCategories() {
			return includedCategories;
		}

		public void setIncludedCategories(List<String> includedCategories) {
			this.includedCategories = includedCategories;
		}

		public List<String> getIncludedProducts() {
			return includedProducts;
		}

		public void setIncludedProducts(List<String> includedProducts) {
			this.includedProducts = includedProducts;
		}

		public List<String> getExcludedSkus() {
			return excludedSkus;
		}

		public void setExcludedSkus(List<String> excludedSkus) {
			this.excludedSkus = excludedSkus;
		}

		public RuleSet getExcludedRuleSet() {
			return excludedRuleSet;
		}

		public void setExcludedRuleSet(RuleSet excludedRuleSet) {
			this.excludedRuleSet = excludedRuleSet;
		}

		public List<String> getExcludedProducts() {
			return excludedProducts;
		}

		public void setExcludedProducts(List<String> excludedProducts) {
			this.excludedProducts = excludedProducts;
		}

		public List<String> getExcludedCategories() {
			return excludedCategories;
		}

		public void setExcludedCategories(List<String> excludedCategories) {
			this.excludedCategories = excludedCategories;
		}

		// Getters and setters

	}

	public static class RuleSet {
		// Currently empty structure
	}

	public static class DiscountStructure {
		@JsonProperty("discount_details")
		private List<DiscountDetail> discountDetails;

		@JsonProperty("discount_type_value")
		private String discountTypeValue;

		public List<DiscountDetail> getDiscountDetails() {
			return discountDetails;
		}

		public void setDiscountDetails(List<DiscountDetail> discountDetails) {
			this.discountDetails = discountDetails;
		}

		public String getDiscountTypeValue() {
			return discountTypeValue;
		}

		public void setDiscountTypeValue(String discountTypeValue) {
			this.discountTypeValue = discountTypeValue;
		}

		// Getters and setters

	}

	public static class DiscountDetail {
		@JsonProperty("spend_value")
		private String spendValue;

		@JsonProperty("discount_value")
		private String discountValue;

		public String getSpendValue() {
			return spendValue;
		}

		public void setSpendValue(String spendValue) {
			this.spendValue = spendValue;
		}

		public String getDiscountValue() {
			return discountValue;
		}

		public void setDiscountValue(String discountValue) {
			this.discountValue = discountValue;
		}

		// Getters and setters

	}

	public static class Translations {
		@JsonProperty("items")
		private List<TranslationItem> items;

		public List<TranslationItem> getItems() {
			return items;
		}

		public void setItems(List<TranslationItem> items) {
			this.items = items;
		}

		// Getters and setters

	}

	public static class TranslationItem {
		@JsonProperty("displayName")
		private String displayName;

		@JsonProperty("description")
		private String description;

		@JsonProperty("lang")
		private String lang;

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}

		// Getters and setters

	}

	public static class Audience {
		@JsonProperty("id")
		private String id;

		@JsonProperty("displayName")
		private String displayName;

		@JsonProperty("deleted")
		private boolean deleted;

		@JsonProperty("enabled")
		private boolean enabled;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public boolean isDeleted() {
			return deleted;
		}

		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		// Getters and setters

	}

	public static class PriceListGroup {
		@JsonProperty("id")
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		// Getters and setters

	}

	public static class ExcludedPromotion {
		@JsonProperty("id")
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		// Getters and setters

	}

	public static class ParentFolder {
		@JsonProperty("id")
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		// Getters and setters

	}

	public static class StackingRule {
		@JsonProperty("id")
		private String id;

		@JsonProperty("displayName")
		private String displayName;

		@JsonProperty("maxPromotions")
		private int maxPromotions;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public int getMaxPromotions() {
			return maxPromotions;
		}

		public void setMaxPromotions(int maxPromotions) {
			this.maxPromotions = maxPromotions;
		}

		// Getters and setters

	}

	public static class ClosenessQualifiers {
		@JsonProperty("matchOnSkuOnly")
		private boolean matchOnSkuOnly;

		@JsonProperty("includeInactiveSkus")
		private boolean includeInactiveSkus;

		public boolean isMatchOnSkuOnly() {
			return matchOnSkuOnly;
		}

		public void setMatchOnSkuOnly(boolean matchOnSkuOnly) {
			this.matchOnSkuOnly = matchOnSkuOnly;
		}

		public boolean isIncludeInactiveSkus() {
			return includeInactiveSkus;
		}

		public void setIncludeInactiveSkus(boolean includeInactiveSkus) {
			this.includeInactiveSkus = includeInactiveSkus;
		}

	}

	public List<Promotion> getPromotion() {
		return promotion;
	}

	public void setPromotion(List<Promotion> promotion) {
		this.promotion = promotion;
	}

	// Getters and setters for PromotionWrapper

}
