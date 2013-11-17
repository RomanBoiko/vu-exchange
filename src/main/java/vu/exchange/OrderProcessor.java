package vu.exchange;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import vu.exchange.MarketRegistrationResult.MarketRegistrationStatus;
import vu.exchange.Markets.MarketDetails;
import vu.exchange.OrderSubmitResult.OrderStatus;

class OrderProcessor {
	private final Map<Long, Book> marketToBook = new HashMap<Long, OrderProcessor.Book>();
	private final Map<Long, MarketDetails> marketIdToMarketDetails = new HashMap<Long, MarketDetails>();

	MarketRegistrationResult registerMarket(MarketRegistrationRequest details) {
		this.marketIdToMarketDetails.put(details.id, new MarketDetails().withId(details.id).withName(details.name));
		if(!this.marketToBook.containsKey(details.id)) {
			this.marketToBook.put(details.id, new Book());
			return new MarketRegistrationResult().withStatus(MarketRegistrationStatus.REGISTERED).withId(details.id);
		}
		return new MarketRegistrationResult().withStatus(MarketRegistrationStatus.UPDATED).withId(details.id);
	}

	Markets availableMarkets(MarketsRequest marketsRequest) {
		Markets markets = new Markets();
		for (Markets.MarketDetails details: marketIdToMarketDetails.values()) {
			markets.addMarket(details);
		}
		return markets;
	}

	MarketPrices marketPrices(MarketPricesRequest pricesRequest) {
		MarketPrices marketPrices =  new MarketPrices().withMarket(pricesRequest.marketId);
		Book book = marketToBook.get(pricesRequest.marketId);
		if(null != book) {
			for(List<Order> bidsList: book.bids.values()) {
				for(Order bid: bidsList) {
					marketPrices.addBid(new MarketPrices.ProductUnit(bid.price, bid.quantity));
				}
			}
			for(List<Order> offersList: book.offers.values()) {
				for(Order offer: offersList) {
					marketPrices.addOffer(new MarketPrices.ProductUnit(offer.price, offer.quantity));
				}
			}
		}
		return marketPrices;
	}

	OrderSubmitResult order(Order order) {
		if(!this.marketToBook.containsKey(order.marketId)) {
			return new OrderSubmitResult().withOrderId(order.id().toString()).withStatus(OrderStatus.REJECTED);
		}
		this.marketToBook.get(order.marketId).addOrder(order);
		return new OrderSubmitResult().withOrderId(order.id().toString()).withStatus(OrderStatus.ACCEPTED);
	}

	private static class Book {

		private TreeMap<BigDecimal, List<Order>> offers = new TreeMap<BigDecimal, List<Order>>();
		private TreeMap<BigDecimal, List<Order>> bids = new TreeMap<BigDecimal, List<Order>>(new DescComparator());
		void addOrder(Order order) {
			if(order.position.equals(Order.Position.BUY)) {
				putOrder(order, bids);
			} else {
				putOrder(order, offers);
			}
		}

		private void putOrder(Order order, Map<BigDecimal, List<Order>> ordersMap) {
			List<Order> orders = ordersMap.get(order.price);
			if (orders == null) {
				orders = new LinkedList<Order>();
				ordersMap.put(order.price, orders);
			}
			orders.add(order);
		}

		private static final class DescComparator implements Comparator<BigDecimal> {
			public int compare(BigDecimal o1, BigDecimal o2) {
				return o2.compareTo(o1);
			}
		}
	}

}