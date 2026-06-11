import React, { useState, useEffect } from 'react';
import './index.css';

interface Customer {
  customerId: string;
  fullName: string;
  email: string;
  phone: string;
}

interface Account {
  accountNumber: string;
  accountType: string;
  balance: number;
  status: string;
}

interface Transaction {
  transactionId: string;
  transactionType: string;
  amount: number;
  balanceAfter: number;
  transactionDate: string;
  description: string;
}

function App() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  const API_BASE = `http://${window.location.hostname}:8080/api`;

  useEffect(() => {
    fetchCustomers();
  }, []);

  const fetchCustomers = async () => {
    try {
      const res = await fetch(`${API_BASE}/customers`);
      const data = await res.json();
      setCustomers(data);
    } catch (err) {
      console.error('Failed to fetch customers', err);
    }
  };

  const handleCustomerSelect = async (customer: Customer) => {
    setSelectedCustomer(customer);
    setSelectedAccount(null);
    setTransactions([]);
    try {
      const res = await fetch(`${API_BASE}/customers/${customer.customerId}/accounts`);
      const data = await res.json();
      setAccounts(data);
    } catch (err) {
      console.error('Failed to fetch accounts', err);
    }
  };

  const handleAccountSelect = async (account: Account) => {
    setSelectedAccount(account);
    fetchTransactions(account.accountNumber);
  };

  const fetchTransactions = async (accountNum: string) => {
    try {
      const res = await fetch(`${API_BASE}/accounts/${accountNum}/transactions`);
      const data = await res.json();
      setTransactions(data);
    } catch (err) {
      console.error('Failed to fetch transactions', err);
    }
  };

  const handleTransaction = async (type: string) => {
    if (!selectedAccount || !amount) return;

    try {
      await fetch(`${API_BASE}/accounts/${selectedAccount.accountNumber}/transactions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          amount: amount,
          transactionType: type,
          description: description || `${type} transaction`,
        }),
      });

      // Refresh account & transactions
      const res = await fetch(`${API_BASE}/accounts/${selectedAccount.accountNumber}`);
      const updatedAccount = await res.json();
      setSelectedAccount(updatedAccount);
      setAccounts(accounts.map(a => a.accountNumber === updatedAccount.accountNumber ? updatedAccount : a));
      
      fetchTransactions(selectedAccount.accountNumber);
      setAmount('');
      setDescription('');
    } catch (err) {
      console.error('Transaction failed', err);
      alert('Transaction failed. Please check balance.');
    }
  };

  return (
    <>
      <header className="header glass">
        <h1>NexusBank Portal</h1>
        <div style={{display: 'flex', gap: '1rem', alignItems: 'center'}}>
          {selectedCustomer && <span>Welcome, {selectedCustomer.fullName}</span>}
          <button className="btn" onClick={() => {
            setSelectedCustomer(null);
            setSelectedAccount(null);
          }}>Home</button>
        </div>
      </header>

      <main className="container">
        {!selectedCustomer ? (
          <div className="animate-fade-in">
            <h2 style={{marginBottom: '1.5rem'}}>Select a Customer</h2>
            <div className="dashboard-grid">
              {customers.map(c => (
                <div key={c.customerId} className="card glass" onClick={() => handleCustomerSelect(c)} style={{cursor: 'pointer'}}>
                  <h2>{c.fullName}</h2>
                  <p style={{color: 'var(--text-secondary)'}}>{c.email}</p>
                  <p style={{color: 'var(--text-secondary)'}}>{c.phone}</p>
                </div>
              ))}
              {customers.length === 0 && <p>No customers found in database. Start the backend with sample data!</p>}
            </div>
          </div>
        ) : (
          <div className="animate-fade-in">
            <div className="dashboard-grid">
              <div className="card glass">
                <h2>Accounts</h2>
                {accounts.map(a => (
                  <div 
                    key={a.accountNumber} 
                    onClick={() => handleAccountSelect(a)}
                    style={{
                      padding: '1rem', 
                      marginTop: '1rem', 
                      backgroundColor: selectedAccount?.accountNumber === a.accountNumber ? 'rgba(99, 102, 241, 0.2)' : 'rgba(0,0,0,0.2)',
                      borderRadius: '8px',
                      cursor: 'pointer',
                      border: selectedAccount?.accountNumber === a.accountNumber ? '1px solid var(--primary)' : '1px solid transparent'
                    }}
                  >
                    <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem'}}>
                      <strong>{a.accountNumber}</strong>
                      <span className="status-positive">${a.balance.toLocaleString()}</span>
                    </div>
                    <small style={{color: 'var(--text-secondary)'}}>{a.accountType}</small>
                  </div>
                ))}
              </div>

              {selectedAccount && (
                <div className="card glass">
                  <h2>Quick Transaction</h2>
                  <div className="form-group">
                    <label>Amount</label>
                    <input 
                      type="number" 
                      className="form-control" 
                      value={amount} 
                      onChange={e => setAmount(e.target.value)} 
                      placeholder="Enter amount..."
                    />
                  </div>
                  <div className="form-group">
                    <label>Description</label>
                    <input 
                      type="text" 
                      className="form-control" 
                      value={description} 
                      onChange={e => setDescription(e.target.value)} 
                      placeholder="Optional note..."
                    />
                  </div>
                  <div style={{display: 'flex', gap: '1rem'}}>
                    <button className="btn btn-success" style={{flex: 1}} onClick={() => handleTransaction('DEPOSIT')}>Deposit</button>
                    <button className="btn btn-danger" style={{flex: 1}} onClick={() => handleTransaction('WITHDRAWAL')}>Withdraw</button>
                  </div>
                </div>
              )}
            </div>

            {selectedAccount && (
              <div className="table-container glass card" style={{marginTop: '2rem'}}>
                <h2>Recent Transactions</h2>
                <table>
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Type</th>
                      <th>Description</th>
                      <th>Amount</th>
                      <th>Balance</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map(t => (
                      <tr key={t.transactionId}>
                        <td>{new Date(t.transactionDate).toLocaleString()}</td>
                        <td>{t.transactionType}</td>
                        <td>{t.description}</td>
                        <td className={t.transactionType.includes('IN') || t.transactionType === 'DEPOSIT' ? 'status-positive' : 'status-negative'}>
                          {t.transactionType.includes('IN') || t.transactionType === 'DEPOSIT' ? '+' : '-'}${t.amount.toLocaleString()}
                        </td>
                        <td>${t.balanceAfter.toLocaleString()}</td>
                      </tr>
                    ))}
                    {transactions.length === 0 && (
                      <tr>
                        <td colSpan={5} style={{textAlign: 'center'}}>No transactions found</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </main>
    </>
  );
}

export default App;
