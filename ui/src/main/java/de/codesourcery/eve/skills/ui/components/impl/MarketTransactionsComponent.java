/**
 * Copyright 2004-2009 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.eve.skills.ui.components.impl;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.MarketTransaction;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.DateHelper;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class MarketTransactionsComponent extends AbstractComponent implements ICharacterSelectionProviderAware
{

	@Resource(name="api-client")
	private IAPIClient apiClient;
	
	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;
	
	private ISelectionProvider<ICharacter> selectionProvider;
	private final MyTableModel model = new MyTableModel();
	private final JTable table = new JTable();
	private final ISelectionListener<ICharacter> selectionListener =
		new ISelectionListener<ICharacter>() {

			@Override
			public void selectionChanged(ICharacter selected)
			{
				refresh();
			}};
	
	private static final class MyTableModel extends AbstractTableModel<MarketTransaction> {

		public static final int TRANSACTION_DATE_IDX = 0;
		public static final int ORDER_TYPE_IDX = 1;
		public static final int ITEMTYPE_IDX= 2;
		public static final int QUANTITY_IDX = 3;
		public static final int PRICE_IDX = 4;
		public static final int CLIENT_NAME_IDX = 5;
		public static final int STATION_NAME_IDX = 6;
		public static final int CORPORATE_TRANSACTION_IDX = 7;
		
		/*
	private EveDate transactionDate;
	private long transactionId;
	private int quantity;
	private InventoryType itemType;
	private ISKAmount price;
	private IClientId clientId;
	private String clientName;
	private Station station;
	private PriceInfo.Type orderType;
	private boolean corporateTransaction;		 
		 */
		
		public MyTableModel() {
			super( new TableColumnBuilder()
			.add("Transaction date")
			.add("Type")
			.add("Item")
			.add("Quantity",Integer.class)
			.add("Price", ISKAmount.class )
			.add("Client")
			.add("Station")
			.add("corporate transaction") );
		}

		private volatile List<MarketTransaction> data =
			new ArrayList<MarketTransaction>();
		
		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final MarketTransaction t= getRow( modelRowIndex );
			switch( modelColumnIndex ) {
				
				case TRANSACTION_DATE_IDX:
					return DateHelper.format( t.getTransactionDate().getLocalTime() );
				case ORDER_TYPE_IDX:
					switch ( t.getOrderType() ) {
						case BUY:
							return "BUY";
						case SELL:
							return "SELL";
						default:
							throw new RuntimeException("Unhandled order type "+t.getOrderType());
					}
				case ITEMTYPE_IDX:
					return t.getItemType().getName();
				case QUANTITY_IDX:
					return t.getQuantity();
				case PRICE_IDX:
					return AmountHelper.formatISKAmount( t.getPrice() );
				case CLIENT_NAME_IDX:
					return t.getClientName();
				case STATION_NAME_IDX:
					return t.getStation().getName();
				case CORPORATE_TRANSACTION_IDX:
					return t.isCorporateTransaction();
				default:
					throw new IllegalArgumentException("Invalid column index "+modelColumnIndex);
			}
		}

		public void setData(List<MarketTransaction> data)
		{
			if ( data == null ) {
				throw new IllegalArgumentException("data cannot be NULL");
			}
			this.data = data;
			modelDataChanged();
		}
		@Override
		public MarketTransaction getRow(int modelRow)
		{
			return data.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}
		
	}
	
	protected void refresh() {
		
		final ICharacter character = selectionProvider.getSelectedItem();
		
		if ( character == null ) {
			model.setData( new ArrayList<MarketTransaction>() );
			return;
		}
		
		submitTask( new UITask() {

			private List<MarketTransaction> transactions;
			@Override
			public String getId()
			{
				return "fetch_market_transactions_for_"+character.getName();
			}

			@Override
			public void run() throws Exception
			{
				
				displayStatus("Fetching market transactions for "+character.getName());
				
				transactions = apiClient.getMarketTransactions(
						character ,
						userAccountStore.getAccountByCharacterID( character.getCharacterId() ) ,
						RequestOptions.DEFAULT ).getPayload();
				
				Collections.sort( transactions , new Comparator<MarketTransaction>() {

					@Override
					public int compare(MarketTransaction o1,
							MarketTransaction o2)
					{
						return o1.getTransactionDate().compareTo( o2.getTransactionDate() );
					}} );
			}
			
			@Override
			public void successHook() throws Exception
			{
				model.setData( transactions );
			}
			
			@Override
			public void failureHook(Throwable t) throws Exception
			{
				displayError("Failed to retrieve market transactions for "+character.getName() , t );
			}
		}
		);
	}
	
	@Override
	protected JPanel createPanel()
	{
		
		final JPanel result =
			new JPanel();
		
		result.setLayout( new GridBagLayout() );
		table.setModel( model );
		table.setRowSorter( model.getRowSorter() );
		result.add( new JScrollPane( table ) , constraints().resizeBoth().useRemainingSpace().end() );
		return result;
	}

	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		selectionProvider.addSelectionListener( selectionListener );
		refresh();
	}
	
	@Override
	protected void onDetachHook()
	{
		selectionProvider.removeSelectionListener(selectionListener);
	}
	
	@Override
	protected void disposeHook()
	{
		selectionProvider.removeSelectionListener(selectionListener);
	}

	public void setSelectionProvider(
			ISelectionProvider<ICharacter> provider)
	{
		if ( provider == null ) {
			throw new IllegalArgumentException("provider cannot be NULL");
		}
		this.selectionProvider = provider;
	}
	
}
