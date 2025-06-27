/*
 * Copyright(C) 2007-2013 National Institute of Informatics, All rights reserved.
 */
package app.checkout;

import java.util.Date;

import app.AppException;
import app.ManagerFactory;
import domain.payment.PaymentManager;
import domain.payment.PaymentException;
import domain.room.RoomManager;
import domain.room.RoomException;

/**
 * Control class for Check-out Customer
 *
 */
public class CheckOutRoomControl {

	public void checkOut(String roomNumber) throws AppException {
		try {
			// RoomManagerとPaymentManagerを取得
			RoomManager roomManager = getRoomManager();
			PaymentManager paymentManager = getPaymentManager();

			// 1. 部屋から顧客情報を削除し、宿泊日を取得する
			// このメソッドは部屋を空にし(stayingDateをnullに設定)、元の宿泊日を返す
			Date stayingDate = roomManager.removeCustomer(roomNumber);

			// 2. 空室数を1つ増やす
			roomManager.updateRoomAvailableQty(stayingDate, 1);

			// 3. 支払い情報を精算済みにする
			paymentManager.consumePayment(stayingDate, roomNumber);
		}
		catch (RoomException e) {
			AppException exception = new AppException("Failed to check-out", e);
			exception.getDetailMessages().add(e.getMessage());
			exception.getDetailMessages().addAll(e.getDetailMessages());
			throw exception;
		}
		catch (PaymentException e) {
			AppException exception = new AppException("Failed to check-out", e);
			exception.getDetailMessages().add(e.getMessage());
			exception.getDetailMessages().addAll(e.getDetailMessages());
			throw exception;
		}
	}

	private RoomManager getRoomManager() {
		return ManagerFactory.getInstance().getRoomManager();
	}

	private PaymentManager getPaymentManager() {
		return ManagerFactory.getInstance().getPaymentManager();
	}
}