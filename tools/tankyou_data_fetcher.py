import pandas as pd
import logging

from datetime import datetime

DEBUG_MODE = False
SEPARATOR = ';'

def setup_logging(debug_mode=False):
    level = logging.DEBUG if debug_mode else logging.INFO

    logging.basicConfig(
        level=level,
        format='%(asctime)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler(),
            logging.FileHandler('tankyou_data_fetcher.log', encoding='utf-8')
        ]
    )
    
    return logging.getLogger(__name__)

def safe_convert_date(date_str, logger, row_id=None):
    try:
        if pd.isna(date_str) or date_str == '':
            return None

        dt = datetime.strptime(str(date_str), '%d/%m/%Y %H:%M:%S')
        return dt.strftime('%Y-%m-%d %H:%M:%S')
    except ValueError as e:
        return str(date_str)
    except Exception as e:
        return None

def process_anagrafica_with_pandas(file_path, flags, logger):
    try:
        df_anagrafica = pd.read_csv(
            file_path, 
            sep=SEPARATOR, 
            encoding='utf-8',
            skiprows=1,
            on_bad_lines='skip'
        )
        
        initial_count = len(df_anagrafica)
        
        required_columns = ['Bandiera', 'Latitudine', 'Longitudine', 'idImpianto']
        missing_columns = [col for col in required_columns if col not in df_anagrafica.columns]
        
        if missing_columns:
            return pd.DataFrame(), []
        
        before_flag_filter = len(df_anagrafica)
        df_anagrafica = df_anagrafica[df_anagrafica['Bandiera'].isin(flags)]
        after_flag_filter = len(df_anagrafica)
        
        before_coord_filter = len(df_anagrafica)
        df_anagrafica = df_anagrafica[
            (df_anagrafica['Latitudine'].notna()) &
            (df_anagrafica['Longitudine'].notna()) &
            (df_anagrafica['Latitudine'] != '') &
            (df_anagrafica['Longitudine'] != '')
        ]
        after_coord_filter = len(df_anagrafica)
        
        if df_anagrafica.empty:
            return pd.DataFrame(), []
        
        output_data = []
        error_count = 0
        
        for idx, row in df_anagrafica.iterrows():
            try:
                station_id = row['idImpianto']
                owner = row.get('Gestore', '')
                flag_name = row['Bandiera']
                station_type = row.get('Tipo Impianto', '')
                name = row.get('Nome Impianto', '')
                address = row.get('Indirizzo', '')
                city = row.get('Comune', '')
                province = row.get('Provincia', '')
                latitude = row['Latitudine']
                longitude = row['Longitudine']
                
                flag_id = flags.index(flag_name) + 1 if flag_name in flags else None
                
                type_mapping = {'Stradale': 1, 'Autostradale': 2}
                type_id = type_mapping.get(station_type, None)
                
                if station_id and flag_id is not None and latitude is not None and longitude is not None:
                    output_data.append({
                        'id': station_id,
                        'owner': owner,
                        'flag': flag_id,
                        'type': type_id,
                        'name': name,
                        'address': address,
                        'city': city,
                        'province': province,
                        'latitude': latitude,
                        'longitude': longitude
                    })
                else:
                    error_count += 1
                    
            except Exception as e:
                error_count += 1
                continue
        
        output_anagrafica = pd.DataFrame(output_data)
        loadedIDs = output_anagrafica['id'].tolist()
        
        return output_anagrafica, loadedIDs
        
    except FileNotFoundError:
        return pd.DataFrame(), []
    except Exception as e:
        return pd.DataFrame(), []

def process_prezzi_with_pandas(file_path, valid_ids, fuels, logger):
    try:
        df_prezzi = pd.read_csv(
            file_path, 
            sep=SEPARATOR, 
            encoding='utf-8', 
            skiprows=1,
            on_bad_lines='skip'
        )
        
        required_columns = ['descCarburante', 'idImpianto', 'prezzo', 'isSelf', 'dtComu']
        missing_columns = [col for col in required_columns if col not in df_prezzi.columns]
        
        if missing_columns:
            return pd.DataFrame()
        
        initial_count = len(df_prezzi)
        
        df_prezzi = df_prezzi[df_prezzi['descCarburante'].isin(fuels)]
        after_fuel_filter = len(df_prezzi)
        
        df_prezzi = df_prezzi[df_prezzi['idImpianto'].isin(valid_ids)]
        after_id_filter = len(df_prezzi)
        
        if df_prezzi.empty:
            return pd.DataFrame()
        
        output_data = []
        error_count = 0
        
        for idx, row in df_prezzi.iterrows():
            try:
                station_id = row['idImpianto']
                fuel_name = row['descCarburante']
                price = row['prezzo']
                is_self = row['isSelf']
                date_str = row['dtComu']
                
                fuel_type = fuels.index(fuel_name) + 1 if fuel_name in fuels else None
                
                last_update = safe_convert_date(date_str, logger, idx)
                
                if station_id and fuel_type is not None and price is not None:
                    output_data.append({
                        'station_id': station_id,
                        'type': fuel_type,
                        'price': price,
                        'self': is_self,
                        'last_update': last_update
                    })
                else:
                    error_count += 1
                    
            except Exception as e:
                error_count += 1
                continue
        
        output_prezzi = pd.DataFrame(output_data)
        
        return output_prezzi
        
    except FileNotFoundError:
        return pd.DataFrame()
    except Exception as e:
        return pd.DataFrame()

def fetchMimitGovData(debug_mode=False):
    logger = setup_logging(debug_mode)
    
    flags = ["Agip Eni", "Api-Ip", "Esso", "Pompe Bianche", "Q8", "Tamoil"]
    fuels = ["Benzina", "Diesel", "Metano", "GPL"]
    
    gsfp = 'anagrafica_impianti_attivi.csv' # Gas Stations File Path
    fpfp = 'prezzo_alle_8.csv' # Fuel Prices File Path
    
    try:
        output_anagrafica, loadedIDs = process_anagrafica_with_pandas(gsfp, flags, logger)
        
        if output_anagrafica.empty:
            return [], pd.DataFrame(), pd.DataFrame()
        
        output_prezzi = process_prezzi_with_pandas(fpfp, loadedIDs, fuels, logger)
        
        try:
            anagrafica_filename = 'gas_stations.csv'
            output_anagrafica.to_csv(anagrafica_filename, index=False, sep=SEPARATOR)
            
            prezzi_filename = 'fuel_prices.csv'
            output_prezzi.to_csv(prezzi_filename, index=False, sep=SEPARATOR)
            
        except Exception as e:
            pass
        
        return loadedIDs, output_anagrafica, output_prezzi
        
    except Exception as e:
        return [], pd.DataFrame(), pd.DataFrame()


if __name__ == "__main__":
    ids, anagrafica_data, prezzi_data = fetchMimitGovData(debug_mode=DEBUG_MODE)