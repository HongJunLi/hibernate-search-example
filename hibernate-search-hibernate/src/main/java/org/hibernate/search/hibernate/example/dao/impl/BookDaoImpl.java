package org.hibernate.search.hibernate.example.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.util.Version;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.hibernate.example.dao.BookDao;
import org.hibernate.search.hibernate.example.model.Author;
import org.hibernate.search.hibernate.example.model.Book;
import org.springframework.stereotype.Repository;

@Repository(value="bookDaoImpl")
public class BookDaoImpl implements BookDao {
	
	@Resource(name="hibernate4sessionFactory")
	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public void add(Book book) {
		this.getSessionFactory().openSession().save(book);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Book> query(int start, int pagesize) {
		Session session = this.getSessionFactory().openSession();
		return session.createCriteria(Book.class).setFirstResult(start).setMaxResults(pagesize).list();
	}

	@Override
	public void update(Book book) {
		this.getSessionFactory().openSession().update(book);
	}

	@Override
	public void delete(Book book) {
		this.getSessionFactory().openSession().delete(book);
	}

	@Override
	public void delete(int id) {
		Object object = this.getSessionFactory().openSession().get(Book.class, id);
		this.getSessionFactory().openSession().delete(object);
	}

	@Override
	public List<Book> query(String keyword, int start, int pagesize,Analyzer analyzer, String... field) throws Exception {
		
		List<Book> books=new ArrayList<Book>();
		
		Session session = this.getSessionFactory().openSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		
		//ʹ��Hibernate Search api��ѯ �Ӷ���ֶ�ƥ�� name��description��authors.name
		//QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity(Book.class ).get();
		//Query luceneQuery = qb.keyword().onFields(field).matching(keyword).createQuery();
		
		//ʹ��lucene api��ѯ �Ӷ���ֶ�ƥ�� name��description��authors.name
		
		MultiFieldQueryParser queryParser=new MultiFieldQueryParser(Version.LUCENE_36,new String[]{"name","description","authors.name"}, analyzer);
		Query luceneQuery=queryParser.parse(keyword);
		
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery);
		fullTextQuery.setFirstResult(start);
		fullTextQuery.setMaxResults(pagesize);
		
		//���ð�id����
		fullTextQuery.setSort(new Sort(new SortField("id", SortField.INT ,true)));
		
		//��������
		SimpleHTMLFormatter formatter=new SimpleHTMLFormatter("<b><font color='red'>", "</font></b>");
		QueryScorer queryScorer=new QueryScorer(luceneQuery);
		Highlighter highlighter=new Highlighter(formatter, queryScorer);

		@SuppressWarnings("unchecked")
		List<Book> tempresult = fullTextQuery.list();
		System.out.println("�����ҵ�["+tempresult.size()+"]����¼");
		for (Book book : tempresult) {
			String highlighterString=null;
			try {
				//����name
				highlighterString=highlighter.getBestFragment(analyzer, "name", book.getName());
				if(highlighterString!=null){
					book.setName(highlighterString);
				}
				//����authors.name
				Set<Author> authors = book.getAuthors();
				for (Author author : authors) {
					highlighterString=highlighter.getBestFragment(analyzer, "authors.name", author.getName());
					if(highlighterString!=null){
						author.setName(highlighterString);
					}
				}
				//����description
				highlighterString=highlighter.getBestFragment(analyzer, "description", book.getDescription());
				if(highlighterString!=null){
					book.setDescription(highlighterString);
				}
			} catch (Exception e) {
			}
			
			books.add(book);
			
			
			System.out.println("����:"+book.getName()+"\n����:"+book.getDescription()+"\n��������:"+book.getPublicationDate());
			System.out.println("----------------------------------------------------------");
		}
		
		fullTextSession.close();
		
		return books;
	}
}