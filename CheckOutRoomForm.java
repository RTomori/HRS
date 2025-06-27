/*
 * Copyright(C) 2007-2013 National Institute of Informatics, All rights reserved.
 */
package app.checkout;

import app.AppException;

/**
 * Form class for Check-out Customer
 * 
 */
public class CheckOutRoomForm {

	private CheckOutRoomControl checkOutRoomControl = new CheckOutRoomControl();

	private CheckOutRoomControl getCheckOutRoomControl() {
		return checkOutRoomControl;
	}

	private String roomNumber;

	public void checkOut() throws AppException {
		/**
		 * Your code for conducting check-out by using some Control object  
		 */
		// コントロールオブジェクトのチェックアウトメソッドを、このフォームが持つ部屋番号を引数にして呼び出します。
		getCheckOutRoomControl().checkOut(getRoomNumber());
	}

	public String getRoomNumber() {
		return roomNumber;
	}

	public void setRoomNumber(String roomNumber) {
		this.roomNumber = roomNumber;
	}

}
