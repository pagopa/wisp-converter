import logging
import os

from logic.clients import APIConfigCacheClient, WispDismantlingDatabase
from utility.constants import Constants
from utility.utility import Utility
from datastructs.configuration import Configuration
from logic.extraction import Extractor

# setting logging level
logging.basicConfig(level=logging.INFO)

# retrieving passed environment variables
report_type = os.getenv("REPORT_TYPE", Constants.DAILY).lower()
date = os.getenv("REPORT_DATE", "")
if date == "":
    date = Utility.get_yesterday_date()
    logging.info(f"\t[INFO ][ActualizeReport] No date passed. Using yesterday date [{date}] as starting date.")
environment = os.getenv("REPORT_ENV", "prod")
if environment.strip() == "":
    environment = "prod"
    logging.info(f"\t[INFO ][ActualizeReport] No environment passed. Using [{environment}] environment as default.")
logging.info(f"\t[INFO ][ActualizeReport] Starting report extraction for date [{date}] for type [{report_type}].")

# initialize parameters
parameters = Utility.load_parameters()

# initialize configuration
configuration = Configuration(
    parameters=parameters,
    env=environment,
    slack_webhook_url=os.getenv("REPORT_SLACK_WEBHOOK_URL"),
    dataexplorer_url=os.getenv("REPORT_DATAEXPLORER_URL"),
    dataexplorer_clientid=os.getenv("REPORT_DATAEXPLORER_CLIENT_ID"),
    dataexplorer_clientsecret=os.getenv("REPORT_DATAEXPLORER_CLIENT_SECRET"),
    dataexplorer_tenantid=os.getenv("REPORT_DATAEXPLORER_TENANT_ID"),
    cosmosdb_url=os.getenv("REPORT_DATABASE_URL"),
    cosmosdb_key=os.getenv("REPORT_DATABASE_KEY"),
    cosmosdb_region=os.getenv("REPORT_DATABASE_REGION", "North Europe"),
    apiconfig_cache_subkey=os.getenv("REPORT_APICONFIG_CACHE_SUBKEY"),
)

# initialize clients
db_client = WispDismantlingDatabase(configuration=configuration)
apiconfig_client = APIConfigCacheClient(configuration=configuration)

logging.info(f"\t[INFO ][ActualizeReport] Actualization of report type [{report_type}] for date [{date}] started! Please wait until process ends.")

# execute report actualization for the passed date
extractor = Extractor(date, parameters, report_type)
entity = db_client.retrieve_report(date=date, type=Constants.DAILY)
entity.get_map()["creditor_institution_info"]["on_wisp"] = 5230
db_client.persist_report(entity)

logging.info(f"\t[INFO ][ActualizeReport] Actualization of report type [{report_type}] for date [{date}] ended!")
