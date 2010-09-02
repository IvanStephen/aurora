package aurora.presentation.component.std;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import uncertain.composite.CompositeMap;
import aurora.application.features.ILookupCodeProvider;
import aurora.presentation.BuildSession;
import aurora.presentation.ViewContext;
import aurora.presentation.component.std.config.ComponentConfig;
import aurora.presentation.component.std.config.DataSetConfig;
import aurora.presentation.component.std.config.DataSetFieldConfig;

/**
 * 
 * @version $Id: DataSet.java v 1.0 2010-8-24 下午01:28:18 IBM Exp $
 * @author <a href="mailto:njq.niu@hand-china.com">vincent</a>
 */
public class DataSet extends Component {
	
	private static final String VALID_SCRIPT = "validscript";
    
	public void onCreateViewContent(BuildSession session, ViewContext context) throws IOException {
		super.onCreateViewContent(session, context);
		CompositeMap view = context.getView();
		CompositeMap model = context.getModel();
		Map map = context.getMap();
		List fieldList = new ArrayList(); 
		
		DataSetConfig dsc = DataSetConfig.getInstance(view);
		CompositeMap fields = dsc.getFields();
		if(fields != null) {
			Iterator it = fields.getChildIterator();
			if(it != null)
			while(it.hasNext()){
				CompositeMap field = (CompositeMap)it.next();
				DataSetFieldConfig sdfc = DataSetFieldConfig.getInstance(field);
//				String validator = sdfc.getValidator();
//				if(!"".equals(validator)) field.putString("validator", validator);
//				field.putString(ComponentConfig.PROPERTITY_NAME, field.getString(ComponentConfig.PROPERTITY_NAME,""));
				if(sdfc.getRequired())field.putBoolean(DataSetFieldConfig.PROPERTITY_REQUIRED, true);
				if(sdfc.getReadOnly())field.putBoolean(DataSetFieldConfig.PROPERTITY_READONLY, true);
//				String dv = field.getString(DataSetFieldConfig.PROPERTITY_DEFAULTVALUE, "");
//				if(!"".equals(dv))field.putString(DataSetFieldConfig.PROPERTITY_DEFAULTVALUE, dv);
				
				String returnField = sdfc.getReturnField();//field.getString(DataSetFieldConfig.PROPERTITY_RETURN_FIELD, "");
				boolean addReturn = returnField!=null;//!"".equals(returnField);
				JSONObject json = new JSONObject(field);
				CompositeMap mapping = sdfc.getMapping();//field.getChild(DataSetConfig.PROPERTITY_MAPPING);
				List maplist = new ArrayList();
				if(mapping != null){
					Iterator mit = mapping.getChildIterator();
					while(mit.hasNext()){
						CompositeMap mapfield = (CompositeMap)mit.next();
						if(returnField!=null && returnField.equals(mapfield.getString("to"))){
							addReturn = false;
						}
						JSONObject mj = new JSONObject(mapfield);
						maplist.add(mj);
					}
				}
				if(addReturn) {
					CompositeMap returnmap = new CompositeMap("map");
					returnmap.putString("from", sdfc.getValueField());//field.getString("valuefield")
					returnmap.putString("to", returnField);
					JSONObject jo = new JSONObject(returnmap);
					maplist.add(jo);
				}
				if(maplist.size() > 0){
					try {
						json.put(DataSetConfig.PROPERTITY_MAPPING, maplist);
					} catch (JSONException e) {
						throw new IOException(e.getMessage());
					}
				}
				fieldList.add(json);
			}
		}
		map.put(DataSetConfig.PROPERTITY_SELECTABLE, new Boolean(dsc.isSelectable()));
		map.put(DataSetConfig.PROPERTITY_SELECTION_MODEL, dsc.getSelectionModel());
		map.put(DataSetConfig.PROPERTITY_FIELDS, fieldList.toString());
		
		StringBuffer sb = new StringBuffer();
		String attachTab = dsc.getValidListener();
		if(attachTab != null){
			String[] ts = attachTab.split(",");
			for(int i=0;i<ts.length;i++){
				String tid = ts[i];
				sb.append("$('"+map.get(ComponentConfig.PROPERTITY_ID)+"').on('valid',function(ds, record, name, valid){if(!valid && !Ext.get('"+tid+"').hasActiveFx()) Ext.get('"+tid+"').frame('ff0000', 3, { duration: 1 })});\n");
			}
		}
		map.put(VALID_SCRIPT, sb.toString());
		
		CompositeMap datas = dsc.getDatas();
		List dataList = new ArrayList(); 
		List list = null;
		if(datas != null){
			String ds = datas.getString(DataSetConfig.PROPERTITY_DATASOURCE, "");
			if(ds.equals("")){
				list = datas.getChilds();
				Iterator dit = list.iterator();
				while(dit.hasNext()){
					CompositeMap item = (CompositeMap)dit.next();
					Iterator it = item.keySet().iterator();
					while(it.hasNext()){
						String key = (String)it.next();
						Object valueKey = item.get(key);
						if(valueKey!=null){
							String value = uncertain.composite.TextParser.parse(valueKey.toString(), model);
							if(value.equals(valueKey.toString())){
								item.put(key, valueKey);							
							}else{
								item.put(key, value);
							}
						}
					}
				}
			}else{
				CompositeMap data= (CompositeMap)model.getObject(ds);
				if(data!= null){
					list = data.getChilds();
				}				
			}
			if(list != null){
				Iterator dit = list.iterator();
				while(dit.hasNext()){
					CompositeMap item = (CompositeMap)dit.next();
					Iterator it = new ArrayList(item.keySet()).iterator();
					while(it.hasNext()){
						String key = (String)it.next();
						Object valueKey = item.get(key);
						if(valueKey==null){
							item.remove(key);
						}
					}
					JSONObject json = new JSONObject(item);
					dataList.add(json);
				}						
			}
		}
		String lcode = dsc.getLookupCode();
		if(lcode!=null){
			ILookupCodeProvider provider = session.getLookupProvider();
			if(provider!=null){
				List llist = new ArrayList();
				try {
					llist = provider.getLookupList(session.getLanguage(), lcode);
				} catch (Exception e) {
					throw new IOException(e.getMessage());
				}
				Iterator it = llist.iterator();
				while(it.hasNext()){
					JSONObject json = new JSONObject((CompositeMap)it.next());
					dataList.add(json);					
				}
			}
		}
		
//		boolean create = dsc.isAutoCreate();
//		if(dataList.size() == 0 && create) {
//			JSONObject json = new JSONObject();
//			dataList.add(json);			
//		}
		map.put(DataSetConfig.PROPERTITY_BINDTARGET, dsc.getBindTarget());
		map.put(DataSetConfig.PROPERTITY_BINDNAME, dsc.getBindName());
		map.put(DataSetConfig.PROPERTITY_PAGEID, session.getSessionContext().getString("pageid", ""));
		map.put(DataSetConfig.PROPERTITY_DATAS, dataList.toString());	
		map.put(DataSetConfig.PROPERTITY_AUTO_QUERY, new Boolean(dsc.isAutoQuery()));
		map.put(DataSetConfig.PROPERTITY_QUERYURL, dsc.getQueryUrl());
		map.put(DataSetConfig.PROPERTITY_SUBMITURL, dsc.getSubmitUrl());
		map.put(DataSetConfig.PROPERTITY_QUERYDATASET, dsc.getQueryDataSet());
		map.put(DataSetConfig.PROPERTITY_FETCHALL, new Boolean(dsc.isFetchAll()));
		map.put(DataSetConfig.PROPERTITY_PAGESIZE, new Integer(dsc.getPageSize()));
		map.put(DataSetConfig.PROPERTITY_AUTO_COUNT, new Boolean(dsc.isAutoCount()));
		map.put(DataSetConfig.PROPERTITY_AUTO_CREATE, new Boolean(dsc.isAutoCreate()));
	}
}
